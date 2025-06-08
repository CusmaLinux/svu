<template>
  <div v-if="attachments && attachments.length > 0">
    <strong><font-awesome-icon icon="paperclip" /> <span v-text="t$('global.field.attachments')"></span></strong>
    <ul class="list-unstyled mt-2">
      <li v-for="file in attachments" :key="file.id">
        <!-- Ajusta esta URL a tu endpoint real de descarga -->
        <a :href="`/api/files/download/${file.id}`" target="_blank" rel="noopener noreferrer">
          <font-awesome-icon icon="file-alt" /> {{ file.nombre }}
        </a>
      </li>
    </ul>
  </div>
</template>

<script lang="ts" src="./attachment-list.component.ts"></script>
