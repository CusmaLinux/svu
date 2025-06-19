<template>
  <div v-if="attachments && attachments.length > 0">
    <strong><font-awesome-icon icon="paperclip" /> <span v-text="t$('global.field.attachments')"></span></strong>
    <ul class="list-unstyled mt-2">
      <li v-for="file in attachments" :key="file.id">
        <b-badge class="text-wrap" variant="light"
          ><b-button
            variant="link"
            class=".btn d-flex justify-content-start align-items-center"
            @click="downloadAttachedFile(file.urlArchivo, file.nombre)"
            ><font-awesome-icon icon="file-alt" /> <span>{{ file.nombre }} </span></b-button
          ></b-badge
        >
      </li>
    </ul>
  </div>
</template>

<script lang="ts" src="./attachment-list.component.ts"></script>

<style lang="postcss">
.btn {
  text-align: left;
}
</style>
