package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.InvalidPqrsStatisticsFilter;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PqrsStatisticsRepository {

    private final MongoTemplate mongo;

    private final long queryTimeoutMs;

    @Autowired
    public PqrsStatisticsRepository(MongoTemplate mongo, @Value("${svu.pqrs-statistics.query-timeout-ms:10000}") long queryTimeoutMs) {
        if (queryTimeoutMs < 1) throw new IllegalArgumentException("Positive statistics query timeout required");
        this.mongo = mongo;
        this.queryTimeoutMs = queryTimeoutMs;
    }

    public PqrsStatisticsRepository(MongoTemplate mongo) {
        this(mongo, 10_000);
    }

    public record Window(Instant previousMonthStart, Instant firstMonthStart, Instant start, Instant cutoff, ZoneId zone) {}

    public record Office(String category, String idType, String id, String name) {}

    public record Bucket(YearMonth month, String type, String state, Office office, long count) {}

    public List<Bucket> institutional(Window window) {
        return aggregate(window, Optional.empty());
    }

    public List<Bucket> office(Window window, String officeId) {
        if (officeId == null || officeId.isBlank()) throw new IllegalArgumentException("Office required");
        List<Object> ids = new ArrayList<>();
        ids.add(officeId);
        if (ObjectId.isValid(officeId)) ids.add(new ObjectId(officeId));
        var matches = mongo
            .getCollection("oficina")
            .find(new Document("_id", new Document("$in", ids)))
            .projection(new Document("_id", 1))
            .limit(2)
            .maxTime(queryTimeoutMs, TimeUnit.MILLISECONDS)
            .into(new ArrayList<>());
        if (matches.size() != 1) throw new InvalidPqrsStatisticsFilter("unknownOrAmbiguousOffice", "Unknown or ambiguous office");
        return aggregate(window, Optional.of(matches.get(0).get("_id")));
    }

    private static Document dates(Instant start, Instant end) {
        return new Document("fecha_creacion", new Document("$gte", Date.from(start)).append("$lt", Date.from(end)));
    }

    private List<Bucket> aggregate(Window w, Optional<Object> officeId) {
        var pipeline = new ArrayList<Document>();
        var match = new Document("$or", List.of(dates(w.previousMonthStart(), w.firstMonthStart()), dates(w.start(), w.cutoff()))).append(
            "$expr",
            Document.parse("{ $eq: [ { $type: '$fecha_creacion' }, 'date' ] }")
        );
        officeId.ifPresent(id -> match.append("oficinaResponder.$id", id));
        pipeline.add(new Document("$match", match));

        // Read DBRef members only on objects. Other historical shapes are unverifiable.
        pipeline.add(
            new Document(
                "$project",
                new Document("_id", 0)
                    .append(
                        "month",
                        new Document(
                            "$dateToString",
                            new Document("date", "$fecha_creacion").append("format", "%Y-%m").append("timezone", w.zone().getId())
                        )
                    )
                    .append("type", typeExpression())
                    .append("state", stateExpression())
                    .append("unassigned", Document.parse("{ $in: [ { $type: '$oficinaResponder' }, ['missing','null'] ] }"))
                    .append(
                        "ref",
                        Document.parse("{ $cond: [ { $eq: [ { $type: '$oficinaResponder' }, 'object' ] }, '$oficinaResponder', {} ] }")
                    )
            )
        );
        pipeline.add(
            new Document(
                "$set",
                new Document("target", refField("$id")).append("collection", refField("$ref")).append("db", refField("$db"))
            )
        );
        var validRef = new Document(
            "$and",
            List.of(
                Document.parse("{ $eq: ['$collection','oficina'] }"),
                new Document("$in", List.of(new Document("$type", "$target"), List.of("objectId", "string"))),
                new Document(
                    "$or",
                    List.of(
                        Document.parse("{ $eq: [ { $type: '$db' }, 'missing' ] }"),
                        new Document("$eq", List.of("$db", mongo.getDb().getName()))
                    )
                )
            )
        );
        pipeline.add(new Document("$set", new Document("target", new Document("$cond", Arrays.asList(validRef, "$target", null)))));
        officeId.ifPresent(id -> pipeline.add(new Document("$match", new Document("target", id))));
        // Group before the catalogue lookup. No child collection is unwound.
        pipeline.add(
            Document.parse(
                "{ $group: { _id: { month:'$month', type:'$type', state:'$state', office:'$target', unassigned:'$unassigned' }, count: { $sum: 1 } } }"
            )
        );
        pipeline.add(
            Document.parse(
                "{ $lookup: { from:'oficina', localField:'_id.office', foreignField:'_id', pipeline:[{$project:{_id:1,nombre:1}}], as:'catalog' } }"
            )
        );
        if (officeId.isPresent()) pipeline.add(new Document("$match", new Document("catalog.0", new Document("$exists", true))));
        return mongo
            .getCollection("pqrs")
            .aggregate(pipeline)
            .maxTime(queryTimeoutMs, TimeUnit.MILLISECONDS)
            .map(row -> {
                Document group = row.get("_id", Document.class);
                List<Document> catalog = row.getList("catalog", Document.class);
                Office office;
                if (group.get("office") != null && !catalog.isEmpty()) {
                    Object id = group.get("office");
                    Object name = catalog.get(0).get("nombre");
                    office = new Office(
                        "ASSIGNED",
                        id instanceof ObjectId ? "objectId" : "string",
                        id.toString(),
                        name instanceof String label ? label : null
                    );
                } else {
                    office = new Office(group.getBoolean("unassigned") ? "UNASSIGNED" : "UNVERIFIABLE", null, null, null);
                }
                return new Bucket(
                    YearMonth.parse(group.getString("month")),
                    group.getString("type"),
                    group.getString("state"),
                    office,
                    ((Number) row.get("count")).longValue()
                );
            })
            .into(new ArrayList<>());
    }

    private static Document refField(String field) {
        return new Document("$getField", new Document("field", new Document("$literal", field)).append("input", "$ref"));
    }

    private static Document typeExpression() {
        List<String> codes = Arrays.stream(PqrsType.values()).map(Enum::name).toList();
        return new Document(
            "$switch",
            new Document(
                "branches",
                List.of(
                    Document.parse(
                        "{ case: { $and: [ { $eq:['$type','REQUEST'] }, { $eq:['$file_number','NOT_ASSIGNED'] } ] }, then:'HISTORICALLY_AMBIGUOUS' }"
                    ),
                    Document.parse("{ case: { $in:[ { $type:'$type' }, ['missing','null'] ] }, then:'MISSING' }"),
                    new Document("case", new Document("$in", List.of("$type", codes))).append("then", "$type")
                )
            ).append("default", "UNKNOWN")
        );
    }

    private static Document stateExpression() {
        var branches = Arrays.stream(PqrsStatus.values())
            .map(state ->
                new Document("case", new Document("$in", List.of("$estado", List.of(state.name(), state.getDisplayName())))).append(
                    "then",
                    state.name()
                )
            )
            .toList();
        return new Document("$switch", new Document("branches", branches).append("default", "UNKNOWN"));
    }
}
