<template>
  <div class="container-fluid mt-4">
    <div v-if="isLoading" class="d-flex justify-content-center my-5">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <!-- Content -->
    <div v-else-if="pqrs" class="row">
      <div class="col-lg-8 mb-4 mb-lg-0">
        <div class="main-content">
          <h1 class="section-title">{{ pqrs.titulo }}</h1>
          <p class="fileNumber-id" v-text="t$('ventanillaUnicaApp.pqrs.detail.fileNumberId') + ': ' + pqrs.fileNumber"></p>
          <hr class="mb-4" />

          <!-- Original Description -->
          <h2 class="section-title" style="font-size: 1.5rem" v-text="t$('ventanillaUnicaApp.pqrs.detail.originalDescription')"></h2>
          <div class="description-box">
            <p>{{ pqrs.descripcion }}</p>
          </div>

          <attachment-list :attachments="pqrs._transientAttachments"></attachment-list>

          <!-- Communication History -->
          <h2 class="section-title" style="font-size: 1.5rem" v-text="t$('ventanillaUnicaApp.pqrs.detail.communicationHistory')"></h2>
          <div v-if="pqrs._transientResponses && pqrs._transientResponses.length > 0">
            <div
              v-for="response in pqrs._transientResponses"
              :key="response.id"
              class="communication-card"
              :class="{ 'support-message': response.byRequester !== false, 'user-message': response.resolver !== null }"
            >
              <div class="message-header">
                <span class="message-author">
                  <font-awesome-icon :icon="['fas', response.byRequester == false ? 'headset' : 'user']" class="me-2" />
                  {{ response.resolver?.login || (response.byRequester !== false ? 'You' : 'Support Team') }}
                </span>
                <span class="message-time">{{ formatDateLong(response.fechaRespuesta) }}</span>
              </div>
              <p class="message-body">{{ response.contenido }}</p>
              <attachment-list :attachments="response._transientAttachments"></attachment-list>
            </div>
          </div>
          <div v-else>
            <p v-text="t$('ventanillaUnicaApp.pqrs.detail.noCommunications')"></p>
          </div>

          <!-- Add a Reply Form -->
          <template v-if="pqrs.estado !== (PqrsStatus.Resolved || PqrsStatus.Closed)">
            <h2 class="section-title mt-4" style="font-size: 1.5rem" v-text="t$('ventanillaUnicaApp.pqrs.detail.addReply')"></h2>
            <form name="replyForm" novalidate @submit.prevent="sendReply">
              <div>
                <div class="form-group">
                  <textarea
                    class="form-control"
                    name="contenido"
                    id="replyMessage"
                    :placeholder="t$('ventanillaUnicaApp.pqrs.detail.replyPlaceholder')"
                    data-cy="contenido"
                    :class="{ valid: !v$.contenido.$invalid, invalid: v$.contenido.$invalid }"
                    v-model="v$.contenido.$model"
                    required
                    rows="5"
                  ></textarea>
                  <div v-if="v$.contenido.$anyDirty && v$.contenido.$invalid">
                    <small class="form-text text-danger" v-for="error of v$.contenido.$errors" :key="error.$uid">{{
                      error.$message
                    }}</small>
                  </div>
                </div>
              </div>
              <div>
                <input type="file" ref="fileInput" @change="onFileChange" multiple style="display: none" />
                <b-button class="mb-3" type="button" variant="primary" @click="triggerFileInput" :disabled="isUploading">
                  Seleccionar archivos
                </b-button>
                <div class="mb-1">
                  <b-list-group class="flex-wrap" horizontal v-if="files?.length > 0">
                    <b-list-group-item class="border" v-for="(file, index) in files" :key="index">
                      <b-badge class="text-wrap" variant="light">{{ file.name }}</b-badge>
                      <b-icon
                        class="cursor-pointer"
                        icon="x-square-fill"
                        variant="danger"
                        role="button"
                        @click="removeFile(index)"
                      ></b-icon>
                    </b-list-group-item>
                  </b-list-group>
                </div>
              </div>
              <div class="mt-3">
                <button
                  type="submit"
                  id="save-entity"
                  data-cy="entityCreateSaveButton"
                  :disabled="v$.$invalid || isSendingReply"
                  class="btn btn-custom-primary"
                >
                  <span v-if="isSendingReply" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  <font-awesome-icon v-else :icon="['fas', 'paper-plane']" class="me-2" />
                  {{ t$('entity.action.send') }}
                </button>
              </div>
            </form>
          </template>
        </div>
      </div>

      <!-- Sidebar Column -->
      <div class="col-lg-4">
        <div class="sidebar">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <h3 class="sidebar-section-title mb-0" v-text="t$('ventanillaUnicaApp.pqrs.detail.fileNumberDetails')"></h3>
            <span class="status-badge" :class="statusClass">{{ pqrs.estado }}</span>
          </div>
          <dl class="sidebar-info">
            <dt v-text="t$('ventanillaUnicaApp.pqrs.fileNumber') + ':'"></dt>
            <dd>{{ pqrs.fileNumber }}</dd>
            <dt v-text="t$('ventanillaUnicaApp.pqrs.type') + ':'"></dt>
            <dd>{{ pqrs.type }}</dd>
            <dt v-text="t$('ventanillaUnicaApp.pqrs.fechaCreacion') + ':'"></dt>
            <dd>{{ formatDateLong(pqrs.fechaCreacion) }}</dd>
            <dt v-text="t$('ventanillaUnicaApp.pqrs.fechaLimiteRespuesta') + ':'"></dt>
            <dd>{{ pqrs.fechaLimiteRespuesta ? formatDateLong(pqrs.fechaLimiteRespuesta) : 'N/A' }}</dd>
            <dt v-text="t$('ventanillaUnicaApp.pqrs.oficinaResponder') + ':'"></dt>
            <dd>{{ pqrs.oficinaResponder?.nombre || 'N/A' }}</dd>
            <dt v-text="t$('ventanillaUnicaApp.pqrs.detail.attachments') + ':'"></dt>
            <dd v-if="pqrs._transientAttachments && pqrs._transientAttachments.length > 0">
              <ul class="list-unstyled mb-0">
                <li v-for="file in pqrs._transientAttachments" :key="file.id">{{ file.nombre }}</li>
              </ul>
            </dd>
            <dd v-else v-text="t$('ventanillaUnicaApp.pqrs.detail.noAttachments')"></dd>
          </dl>

          <!-- Actions -->
          <h3 class="sidebar-section-title mt-4" v-text="t$('entity.action.title')"></h3>
          <button class="btn btn-custom-success w-100" @click="goBack">
            <font-awesome-icon :icon="['fas', 'arrow-left']" class="me-2" />
            {{ t$('entity.action.back') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./public-pqrs-details.component.ts"></script>

<style scoped>
.main-content {
  background-color: #ffffff;
  padding: 2rem;
  border-radius: 0.5rem;
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.05);
}
.sidebar {
  background-color: #e9ecef;
  padding: 2rem;
  border-radius: 0.5rem;
}
.section-title {
  font-size: 1.75rem;
  font-weight: 500;
  margin-bottom: 1rem;
  color: #343a40;
}
.fileNumber-id {
  font-size: 0.9rem;
  color: #6c757d;
}
.description-box {
  background-color: #f8f9fa;
  padding: 1.5rem;
  border-radius: 0.375rem;
  border: 1px solid #dee2e6;
  margin-bottom: 1.5rem;
}
.communication-card {
  border-radius: 0.375rem;
  margin-bottom: 1.5rem;
  padding: 1.5rem;
}
.support-message {
  background-color: #e6f7f2;
  border-left: 4px solid #198754;
}
.user-message {
  background-color: #e9ecef;
  border-left: 4px solid #0d6efd;
}
.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.message-author {
  font-weight: bold;
  color: #495057;
}
.message-time {
  font-size: 0.85rem;
  color: #6c757d;
}
.message-body {
  color: #495057;
  white-space: pre-wrap;
}
.sidebar-section-title {
  font-size: 1.25rem;
  font-weight: 500;
  color: #343a40;
  margin-bottom: 1rem;
  border-bottom: 1px solid #ced4da;
  padding-bottom: 0.5rem;
}
.sidebar-info dt {
  font-weight: bold;
  color: #495057;
}
.sidebar-info dd {
  color: #6c757d;
  margin-bottom: 0.75rem;
}
.btn-custom-success {
  background-color: #28a745;
  border-color: #28a745;
  color: white;
}
.btn-custom-success:hover {
  background-color: #218838;
  border-color: #1e7e34;
}
.btn-custom-primary {
  background-color: #0d6efd;
  border-color: #0d6efd;
  color: white;
}
.btn-custom-primary:hover {
  background-color: #0b5ed7;
  border-color: #0a58ca;
}
.status-badge {
  display: inline-block;
  padding: 0.35em 0.65em;
  font-size: 0.75em;
  font-weight: 700;
  line-height: 1;
  color: #fff;
  text-align: center;
  white-space: nowrap;
  vertical-align: baseline;
  border-radius: 0.25rem;
  float: right;
}
.status-open {
  background-color: #ffc107;
  color: #000;
}
.status-in-progress {
  background-color: #0dcaf0;
}
.status-closed {
  background-color: #5cb85c;
}
.status-default {
  background-color: #6c757d;
}
</style>
