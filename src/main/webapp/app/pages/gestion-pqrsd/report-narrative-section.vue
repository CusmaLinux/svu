<script setup lang="ts">
import { narrativeLimit, narrativeSections, type NarrativeField, type NarrativeSection, type ReportNarrative } from './report-narrative';

defineProps<{ section: NarrativeSection; fields: ReportNarrative; reading: boolean }>();
const emit = defineEmits<{ edit: [field: NarrativeField, text: string] }>();

function edit(field: NarrativeField, event: Event) {
  const control = event.target as HTMLTextAreaElement;
  const text = control.value.slice(0, narrativeLimit);
  if (control.value !== text) control.value = text;
  emit('edit', field, text);
}
</script>

<template>
  <div class="report-narrative">
    <div v-for="{ id, label } in narrativeSections[section]" :key="id" class="narrative-field">
      <template v-if="reading">
        <h4>{{ label }}</h4>
        <p v-if="fields[id]?.trim()" class="narrative-text" :data-narrative="id">{{ fields[id] }}</p>
        <p v-else class="text-muted mb-0">Sin texto redactado.</p>
      </template>
      <template v-else>
        <label :for="'narrative-' + id">{{ label }}</label>
        <textarea
          :id="'narrative-' + id"
          class="form-control"
          rows="3"
          :value="fields[id] ?? ''"
          :maxlength="narrativeLimit"
          :aria-describedby="'narrative-count-' + id"
          @input="edit(id, $event)"
        ></textarea>
        <small :id="'narrative-count-' + id" class="d-block text-right text-muted">
          {{ fields[id]?.length ?? 0 }} / {{ narrativeLimit }} caracteres
        </small>
      </template>
    </div>
  </div>
</template>

<style scoped>
.report-narrative {
  margin-top: 1rem;
  padding: clamp(0.75rem, 2vw, 1rem);
  background: #f4f9f9;
  border: 1px solid #d4e3e4;
  border-left: 3px solid #24867c;
  border-radius: 0.45rem;
}
.narrative-field + .narrative-field {
  margin-top: 0.9rem;
}
label,
h4 {
  font-size: 1rem;
  font-weight: 650;
}
textarea {
  resize: vertical;
}
.narrative-text {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
