<template>
  <b-container>
    <b-card no-body class="shadow-lg rounded-lg overflow-hidden border-0">
      <div class="p-4 p-md-5">
        <!-- Header -->
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-start align-items-md-center mb-4">
          <h1 class="h4 font-weight-bold text-dark mb-3 mb-md-0">
            {{ pqrs?.titulo }}
            <span class="text-muted font-weight-normal">#{{ pqrs?.fileNumber }}</span>
          </h1>
        </div>

        <!-- Main Content -->
        <b-row>
          <!-- Left Column -->
          <b-col md="8">
            <!-- General Information Card -->
            <div class="bg-white p-4 rounded shadow-sm border mb-4">
              <h2 class="h5 font-weight-bold text-dark mb-4"><font-awesome-icon icon="circle-info" /> Información General</h2>
              <div>
                <div class="mb-3">
                  <p class="small font-weight-bold text-muted mb-1">Descripción</p>
                  <p class="text-dark mb-0">{{ pqrs?.descripcion }}</p>
                </div>
                <hr />
                <b-row class="my-3">
                  <b-col sm="6" class="mb-3 mb-sm-0">
                    <p class="small font-weight-bold text-muted mb-1">Fecha de Creación</p>
                    <p class="text-dark mb-0">{{ formatDateLong(pqrs?.fechaCreacion) }}</p>
                  </b-col>
                  <b-col sm="6">
                    <p class="small font-weight-bold text-muted mb-1">Fecha Límite de Respuesta</p>
                    <b-badge variant="warning" pill class="font-weight-bold px-2 py-1">
                      {{ formatDateLong(pqrs?.fechaLimiteRespuesta) }}
                    </b-badge>
                  </b-col>
                </b-row>
                <hr />
                <b-row class="mt-3">
                  <b-col sm="6" class="mb-3 mb-sm-0">
                    <p class="small font-weight-bold text-muted mb-1">Estado</p>
                    <b-badge pill class="d-inline-flex align-items-center font-weight-bold px-2 py-1" :class="statusClass">
                      <font-awesome-icon icon="clock" class="mr-1" />
                      {{ pqrs?.estado }}
                    </b-badge>
                  </b-col>
                  <b-col sm="6">
                    <p class="small font-weight-bold text-muted mb-1">Oficina Responsable</p>
                    <div v-if="pqrs?.oficinaResponder">
                      <router-link :to="{ name: 'OficinaView', params: { oficinaId: pqrs.oficinaResponder.id } }">{{
                        pqrs.oficinaResponder.nombre || pqrs.oficinaResponder.id
                      }}</router-link>
                    </div>
                  </b-col>
                </b-row>
              </div>
            </div>

            <!-- Attachments of pqrs -->
            <div v-if="pqrs?._transientAttachments" class="bg-white p-4 rounded shadow-sm border mb-4">
              <b-list-group>
                <attachment-list :attachments="pqrs._transientAttachments"></attachment-list>
              </b-list-group>
              <p class="small text-muted mt-3">Total: {{ pqrs._transientAttachments.length }} archivos</p>
            </div>

            <!-- Responses of pqrs -->
            <div v-if="pqrs?._transientResponses" class="bg-white p-4 rounded shadow-sm border">
              <b-list-group>
                <strong class="mb-4"><font-awesome-icon icon="comment" /> <span>Historial de Comunicación </span></strong>
                <div v-for="response in pqrs._transientResponses" :key="response.id">
                  <response-item :response="response"></response-item>
                </div>
              </b-list-group>
            </div>
          </b-col>

          <!-- Right Column (Actions) -->
          <b-col md="4" class="mt-4 mt-md-0">
            <sidebar
              v-if="pqrs"
              :pqrs="pqrs"
              :is-functionary="isFunctionary"
              :is-admin="isAdmin"
              :is-frontdesk="isFrontdesk"
              @toggle-status="toggleStatusPqrs"
              @open-close-modal="openConfirmCloseModal"
            />
          </b-col>
        </b-row>
      </div>
    </b-card>
    <b-modal
      id="confirmClosePqrsModal"
      ref="confirmCloseModalRef"
      v-model="isConfirmCloseModalVisible"
      :title="t('ventanillaUnicaApp.pqrs.messages.confirmClosePqrs')"
      @ok="handleConfirmClose"
      :ok-title="t('entity.action.confirm')"
      ok-variant="danger"
      :cancel-title="t('entity.action.cancel')"
      cancel-variant="secondary"
      centered
    >
      <p v-text="t('ventanillaUnicaApp.pqrs.messages.messageClosePqrs')"></p>
    </b-modal>
  </b-container>
</template>

<script lang="ts" src="./pqrs-details.component.ts"></script>
<style scoped lang="scss">
.bg-white.p-4 {
  background-color: #fff !important;
}

.status-open {
  background-color: #ffc107;
  color: #000;
}
.status-in-progress {
  background-color: #0dcaf0;
}
.status-closed {
  background-color: #6c757d;
}
.status-default {
  background-color: #6c757d;
}

.badge {
  font-size: 14px;
}
</style>
