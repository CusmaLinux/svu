<script setup lang="ts">
defineProps<{ caption: string; columns: string[]; rows: string[][] }>();
</script>

<template>
  <div class="table-responsive" tabindex="0" :aria-label="caption" role="region">
    <table class="table table-sm mb-0">
      <caption class="sr-only">
        {{
          caption
        }}
      </caption>
      <thead>
        <tr>
          <th v-for="(column, index) in columns" :key="index" scope="col">{{ column }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in rows" :key="index">
          <th scope="row">{{ row[0] }}</th>
          <td v-for="(cell, column) in row.slice(1)" :key="column">{{ cell }}</td>
        </tr>
        <tr v-if="!rows.length">
          <td :colspan="columns.length">Sin registros en el periodo.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
th,
td {
  padding: 0.55rem 0.75rem;
  white-space: nowrap;
}
thead {
  background: #eef4f5;
  color: #18383e;
}
tbody th {
  font-weight: 500;
}
</style>
