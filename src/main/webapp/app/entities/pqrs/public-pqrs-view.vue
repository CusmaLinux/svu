<template>
  <div class="container mt-4">
    <div class="row justify-content-center">
      <div class="col-md-10">
        <div v-if="loading" class="text-center">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p class="mt-2">Cargando información de la PQRS...</p>
        </div>

        <div v-if="error" class="alert alert-danger">
          <p>No se pudo cargar la PQRS. Es posible que el enlace no sea válido o haya expirado.</p>
        </div>

        <div v-if="pqrs">
          <!-- Detalles de la PQRS -->
          <div class="card mb-4">
            <div class="card-header">
              <h2 class="mb-0"><span v-text="t$('ventanillaUnicaApp.pqrs.detail.title')"></span>: {{ pqrs.id }}</h2>
            </div>
            <div class="card-body">
              <dl class="row jh-entity-details">
                <dt class="col-sm-3"><span v-text="t$('ventanillaUnicaApp.pqrs.titulo')"></span></dt>
                <dd class="col-sm-9">{{ pqrs.titulo }}</dd>

                <dt class="col-sm-3"><span v-text="t$('ventanillaUnicaApp.pqrs.descripcion')"></span></dt>
                <dd class="col-sm-9" style="white-space: pre-wrap">{{ pqrs.descripcion }}</dd>

                <dt class="col-sm-3"><span v-text="t$('ventanillaUnicaApp.pqrs.estado')"></span></dt>
                <dd class="col-sm-9">
                  <b-badge variant="info" pill>{{ pqrs.estado }}</b-badge>
                </dd>

                <dt class="col-sm-3"><span v-text="t$('ventanillaUnicaApp.pqrs.fechaCreacion')"></span></dt>
                <dd class="col-sm-9">{{ formatDateLong(pqrs.fechaCreacion) }}</dd>

                <dt class="col-sm-3"><span v-text="t$('ventanillaUnicaApp.pqrs.fechaLimiteRespuesta')"></span></dt>
                <dd class="col-sm-9">
                  <strong>{{ formatDateLong(pqrs.fechaLimiteRespuesta) }}</strong>
                </dd>
              </dl>
              <hr v-if="pqrs._transientAttachments && pqrs._transientAttachments.length > 0" />
              <AttachmentList :attachments="pqrs._transientAttachments" />
            </div>
          </div>

          <!-- Historial de Conversación -->
          <div class="conversation-history">
            <h3 class="mb-3">Historial de Conversación</h3>
            <div v-if="pqrs._transientResponses && pqrs._transientResponses.length > 0">
              <ResponseItem v-for="response in sortedResponses" :key="response.id" :response="response" />
            </div>
            <div v-else class="alert alert-secondary">
              <p>Aún no hay respuestas para esta PQRS.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" src="./public-pqrs-view.component.ts"></script>
