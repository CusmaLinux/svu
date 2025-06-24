<template>
  <div class="card mb-3" :class="{ 'bg-light-blue': response.resolver !== null ? true : false }">
    <div class="card-header d-flex justify-content-between">
      <strong v-if="response.resolver">
        <font-awesome-icon icon="headset" /> <span>{{ response.resolver.login }}</span>
      </strong>
      <strong v-else> <font-awesome-icon icon="user" /> <span>Solicitante</span> </strong>
      <small class="text-muted">{{ formatDateLong(response.fechaRespuesta) }}</small>
    </div>
    <div class="card-body">
      <p class="card-text" v-text="response.contenido"></p>
      <hr v-if="response._transientAttachments && response._transientAttachments.length > 0" />
      <attachment-list :attachments="response._transientAttachments" />
    </div>
  </div>
</template>

<script lang="ts" src="./response-item.component.ts"></script>

<style scoped>
.bg-light-blue {
  background-color: #ecf8f8;
}

.card {
  border-radius: 1rem;
}
</style>
