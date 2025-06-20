<template>
  <div class="card mb-3" :class="{ 'bg-light': response.byRequester }">
    <div class="card-header d-flex justify-content-between">
      <strong v-if="!response.byRequester">
        <font-awesome-icon icon="headset" /> <span v-text="t$('ventanillaUnicaApp.respuesta.responseFromStaff')"></span>
      </strong>
      <strong v-else>
        <font-awesome-icon icon="user" /> <span v-text="t$('ventanillaUnicaApp.respuesta.responseFromRequester')"></span>
      </strong>
      <small class="text-muted">{{ formatDateLong(response.fechaRespuesta) }}</small>
    </div>
    <div class="card-body">
      <p class="card-text" v-html="response.contenido"></p>
      <hr v-if="response._transientAttachments && response._transientAttachments.length > 0" />
      <AttachmentList :attachments="response._transientAttachments" />
    </div>
  </div>
</template>

<script lang="ts" src="./response-item.component.ts"></script>
