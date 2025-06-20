package co.edu.itp.svu.service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import co.edu.itp.svu.domain.RequirementSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    @Autowired
    public SequenceGeneratorService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    /**
     * Atomically finds and increments a sequence.
     *
     * @param seqName The unique identifier for the sequence (e.g., "PQRS_2025" | "PQRS_2025_Q3").
     * @return The next number in the sequence.
     */
    public long getNextSequence(String seqName) {
        RequirementSequence counter = mongoOperations.findAndModify(
            query(where("_id").is(seqName)),
            new Update().inc("seq", 1),
            options().returnNew(true).upsert(true),
            RequirementSequence.class
        );

        return counter.getSeq();
    }
}
