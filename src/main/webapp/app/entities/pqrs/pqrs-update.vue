<template>
  <b-container>
    <div class="row justify-content-center">
      <div class="col-md-10 col-lg-9">
        <b-card no-body class="shadow-sm border-0">
          <b-card-header class="bg-light py-3">
            <h2
              id="ventanillaUnicaApp.pqrs.home.createOrEditLabel"
              data-cy="PqrsCreateUpdateHeading"
              class="text-center mb-0 h3 font-weight-bold"
            >
              {{ pqrs.id ? 'Editar PQRSD' : 'Crear Nueva PQRSD' }}
            </h2>
          </b-card-header>

          <b-card-body class="p-4">
            <b-alert v-if="pqrs.id" show variant="primary" class="d-flex align-items-center mb-4">
              <font-awesome-icon icon="pencil-alt" class="fa-2x mr-3" />
              <div>
                <strong>Modo Edición:</strong> Está gestionando la PQRSD con radicado <strong>{{ pqrs.fileNumber }}</strong
                >. Actualice su estado, adjunte archivos o asigne la gestión a la oficina correspondiente.
              </div>
            </b-alert>

            <b-alert v-else show variant="success" class="d-flex align-items-center mb-4">
              <font-awesome-icon icon="plus-circle" class="fa-2x mr-3" />
              <div>
                <strong>Modo Creación:</strong> Complete los datos para registrar una nueva PQRSD. Los campos de gestión interna (estado,
                fechas, etc.) se habilitarán después de guardar por primera vez.
              </div>
            </b-alert>

            <b-form @submit.prevent="save">
              <b-form-group v-if="pqrs.id" :label="t$('global.field.id')" label-for="id" label-class="font-weight-bold">
                <b-form-input id="id" v-model="pqrs.id" readonly></b-form-input>
              </b-form-group>

              <b-row>
                <b-col md="6">
                  <b-form-group label-for="requesterEmail" label-class="font-weight-bold">
                    <template #label> Correo del Solicitante </template>
                    <b-input-group>
                      <b-input-group-prepend is-text><font-awesome-icon icon="at" /></b-input-group-prepend>
                      <b-form-input
                        id="requesterEmail"
                        v-model="requesterEmailModel"
                        type="email"
                        :state="v$.requesterEmail.$dirty ? !v$.requesterEmail.$error : null"
                      ></b-form-input>
                    </b-input-group>
                    <b-form-invalid-feedback
                      :state="!v$.requesterEmail.$error"
                      v-for="error of v$.requesterEmail.$errors"
                      :key="error.$uid"
                    >
                      {{ error.$message }}
                    </b-form-invalid-feedback>
                  </b-form-group>
                </b-col>
                <b-col md="6">
                  <b-form-group :label="t$('ventanillaUnicaApp.pqrs.public.type')" label-for="pqrs-type" label-class="font-weight-bold">
                    <template #label> Tipo <span class="text-danger">*</span> </template>
                    <b-input-group>
                      <b-input-group-prepend is-text><font-awesome-icon icon="list-ul" /></b-input-group-prepend>
                      <b-form-select
                        id="pqrs-type"
                        v-model="v$.type.$model"
                        :options="pqrsTypeOptions"
                        :state="v$.type.$dirty ? !v$.type.$error : null"
                      ></b-form-select>
                    </b-input-group>
                    <b-form-invalid-feedback :state="!v$.type.$error" v-for="error of v$.type.$errors" :key="error.$uid">
                      {{ error.$message }}
                    </b-form-invalid-feedback>
                  </b-form-group>
                </b-col>
              </b-row>

              <b-form-group label-for="pqrs-titulo" label-class="font-weight-bold">
                <template #label> Título <span class="text-danger">*</span> </template>
                <b-input-group>
                  <b-input-group-prepend is-text><font-awesome-icon icon="tag" /></b-input-group-prepend>
                  <b-form-input
                    id="pqrs-titulo"
                    v-model.trim="v$.titulo.$model"
                    :state="v$.titulo.$dirty ? !v$.titulo.$error : null"
                  ></b-form-input>
                </b-input-group>
                <b-form-invalid-feedback :state="!v$.titulo.$error" v-for="error of v$.titulo.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group label-for="pqrs-descripcion" label-class="font-weight-bold">
                <template #label> Descripción <span class="text-danger">*</span> </template>
                <b-form-textarea
                  id="pqrs-descripcion"
                  v-model.trim="v$.descripcion.$model"
                  rows="5"
                  :state="v$.descripcion.$dirty ? !v$.descripcion.$error : null"
                ></b-form-textarea>
                <b-form-invalid-feedback :state="!v$.descripcion.$error" v-for="error of v$.descripcion.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group :label="t$('ventanillaUnicaApp.pqrs.archivosAdjuntos', 'Archivos adjuntos')" label-class="font-weight-bold">
                <input type="file" ref="fileInput" @change="onFileChange" multiple style="display: none" />
                <div class="file-drop-zone" @click="triggerFileInput" @dragover.prevent @dragleave.prevent @drop.prevent="onDrop">
                  <font-awesome-icon icon="cloud-arrow-up" class="fa-3x text-secondary mb-2" />
                  <p class="mb-0">Arrastre archivos aquí o haga clic para seleccionar</p>
                </div>
                <transition-group name="list" tag="div" class="mt-3">
                  <!-- Existing Files (from edit mode) -->
                  <b-list-group-item
                    v-for="(file, index) in existingFilesInfo"
                    :key="file.id"
                    class="d-flex justify-content-between align-items-center"
                  >
                    <div>
                      <font-awesome-icon icon="paperclip" class="text-muted mr-2" />
                      <b-link @click="downloadAttachedFile(file.urlArchivo, file.nombre)">{{ file.nombre }}</b-link>
                    </div>
                    <b-button variant="link" class="p-0 text-danger" @click="removeExistingFile(index)" v-b-tooltip.hover title="Eliminar">
                      <font-awesome-icon icon="times-circle" />
                    </b-button>
                  </b-list-group-item>
                  <!-- New Files (to be uploaded) -->
                  <b-list-group-item
                    v-for="(file, index) in files"
                    :key="file.name"
                    class="d-flex justify-content-between align-items-center"
                  >
                    <span><font-awesome-icon icon="paperclip" class="text-muted mr-2" />{{ file.name }}</span>
                    <b-button variant="link" class="p-0 text-danger" @click="removeFile(index)" v-b-tooltip.hover title="Eliminar">
                      <font-awesome-icon icon="times-circle" />
                    </b-button>
                  </b-list-group-item>
                </transition-group>
              </b-form-group>

              <!-- Section for internal management fields, only shown in edit mode -->
              <div v-if="isUpdateMode">
                <hr class="my-4" />
                <h4 class="mb-3 text-secondary">Gestión Interna</h4>

                <b-row>
                  <b-col md="6">
                    <b-form-group label="Oficina Responsable" label-for="pqrs-oficinaResponder" label-class="font-weight-bold">
                      <b-input-group>
                        <b-input-group-prepend is-text><font-awesome-icon icon="sitemap" /></b-input-group-prepend>
                        <b-form-select id="pqrs-oficinaResponder" v-model="pqrs.oficinaResponder" :options="oficinaOptions"></b-form-select>
                      </b-input-group>
                    </b-form-group>
                  </b-col>
                  <b-col md="6">
                    <b-form-group label="Estado" label-for="pqrs-estado" label-class="font-weight-bold">
                      <b-input-group>
                        <b-input-group-prepend is-text><font-awesome-icon icon="check-circle" /></b-input-group-prepend>
                        <b-form-select
                          id="pqrs-estado"
                          v-model="v$.estado.$model"
                          :options="statesPqrsOptions"
                          :disabled="pqrs.estado === 'Cerrado'"
                          :state="v$.estado.$dirty ? !v$.estado.$error : null"
                        ></b-form-select>
                      </b-input-group>
                    </b-form-group>
                  </b-col>
                </b-row>

                <b-row>
                  <b-col md="6" v-can="['assign_due_date', 'pqrs']">
                    <b-form-group label="Días Hábiles para Respuesta" label-for="pqrs-daysToReply" label-class="font-weight-bold">
                      <b-input-group>
                        <b-input-group-prepend is-text><font-awesome-icon icon="hourglass-half" /></b-input-group-prepend>
                        <b-form-input
                          id="pqrs-daysToReply"
                          v-model.number="v$.daysToReply.$model"
                          type="number"
                          min="0"
                          max="365"
                        ></b-form-input>
                      </b-input-group>
                    </b-form-group>
                  </b-col>
                  <b-col md="6">
                    <b-form-group label="Fecha de Creación" label-for="pqrs-fechaCreacion" label-class="font-weight-bold">
                      <b-input-group>
                        <b-input-group-prepend is-text><font-awesome-icon icon="calendar-day" /></b-input-group-prepend>
                        <b-form-input
                          :value="convertDateTimeFromServer(v$.fechaCreacion.$model)"
                          readonly
                          type="datetime-local"
                        ></b-form-input>
                      </b-input-group>
                    </b-form-group>
                  </b-col>
                  <b-col md="6" v-can="['assign_due_date', 'pqrs']">
                    <b-form-group label="Fecha Límite de Respuesta" label-for="pqrs-fechaLimite" label-class="font-weight-bold">
                      <b-input-group>
                        <b-input-group-prepend is-text><font-awesome-icon icon="calendar-check" /></b-input-group-prepend>
                        <b-form-input
                          id="pqrs-fechaLimite"
                          type="datetime-local"
                          data-cy="fechaLimiteRespuesta"
                          name="fechaLimiteRespuesta"
                          :value="convertDateTimeFromServer(v$.fechaLimiteRespuesta.$model)"
                          @change="updateInstantField('fechaLimiteRespuesta', $event)"
                          :state="v$.fechaLimiteRespuesta.$dirty ? !v$.fechaLimiteRespuesta.$error : null"
                        ></b-form-input>
                      </b-input-group>
                    </b-form-group>
                  </b-col>
                </b-row>
              </div>

              <div class="d-flex justify-content-end mt-4 border-top pt-4">
                <b-button variant="secondary" class="mr-3" @click="previousState()">
                  <font-awesome-icon icon="ban" />
                  <span class="ml-1">Cancelar</span>
                </b-button>
                <b-button
                  variant="primary"
                  type="submit"
                  :disabled="v$.$invalid || isSaving || isUploading"
                  data-cy="entityCreateSaveButton"
                  style="min-width: 120px"
                >
                  <span v-if="isSaving">
                    <b-spinner small></b-spinner>
                    <span class="ml-2">Guardando...</span>
                  </span>
                  <span v-else>
                    <font-awesome-icon icon="save" />
                    <span class="ml-1">Guardar</span>
                  </span>
                </b-button>
              </div>
            </b-form>
          </b-card-body>
        </b-card>
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./pqrs-update.component.ts"></script>

<style scoped>
/* Reusing the same styles from our polished public form */
.file-drop-zone {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.file-drop-zone:hover {
  background-color: #f8f9fa;
  border-color: #007bff;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}
.list-enter,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.b-form-group {
  margin-bottom: 1.5rem;
}
</style>
