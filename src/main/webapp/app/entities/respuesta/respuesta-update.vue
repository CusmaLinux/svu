<template>
  <b-container class="py-5">
    <b-row class="justify-content-center">
      <b-col md="10" lg="8">
        <b-card no-body class="shadow-sm border-0">
          <b-card-header class="bg-light py-3">
            <h2
              id="ventanillaUnicaApp.respuesta.home.createOrEditLabel"
              data-cy="RespuestaCreateUpdateHeading"
              class="text-center mb-0 h3 font-weight-bold"
            >
              {{ respuesta.id ? 'Modificar la respuesta' : 'Responder a la solicitud' }}
            </h2>
          </b-card-header>

          <b-card-body>
            <b-form @submit.prevent="save()">
              <b-alert show variant="info" v-if="selectedPqrsInfo" class="d-flex align-items-center mb-4">
                <font-awesome-icon icon="circle-info" class="fa-2x mr-3" />
                <div>
                  <span v-if="!respuesta.id" v-text="t$('ventanillaUnicaApp.respuesta.respondingToPqrs')"></span>
                  <span v-else v-text="t$('ventanillaUnicaApp.respuesta.associatedPqrs')"></span>:
                  <strong>{{ selectedPqrsInfo.titulo || selectedPqrsInfo.id }}</strong>
                </div>
              </b-alert>

              <b-form-group v-if="respuesta.id" :label="t$('global.field.id')" label-for="respuesta-id" label-class="font-weight-bold">
                <b-form-input id="respuesta-id" v-model="respuesta.id" readonly></b-form-input>
              </b-form-group>

              <b-form-group id="respuesta-contenido-group" label-for="respuesta-contenido" label-class="font-weight-bold">
                <template #label> {{ t$('ventanillaUnicaApp.respuesta.contenido') }} <span class="text-danger">*</span> </template>
                <b-form-textarea
                  id="respuesta-contenido"
                  v-model.trim="v$.contenido.$model"
                  :state="v$.contenido.$dirty ? !v$.contenido.$error : null"
                  required
                  rows="5"
                  data-cy="contenido"
                  placeholder="Escriba aquí el contenido de la respuesta..."
                ></b-form-textarea>
                <b-form-invalid-feedback :state="!v$.contenido.$error" v-for="error of v$.contenido.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group label-class="font-weight-bold" :label="t$('ventanillaUnicaApp.pqrs.archivosAdjuntos', 'Archivos adjuntos')">
                <input type="file" ref="fileInput" @change="onFileChange" multiple style="display: none" />
                <div
                  class="file-drop-zone"
                  @click="triggerFileInput"
                  @dragover.prevent
                  @dragleave.prevent
                  @drop.prevent="onDrop"
                  :class="{ 'is-uploading': isUploading }"
                >
                  <font-awesome-icon icon="cloud-arrow-up" class="fa-3x text-secondary mb-2" />
                  <p class="mb-0">
                    {{ isUploading ? 'Subiendo...' : 'Arrastre los archivos aquí o haga clic para seleccionar' }}
                  </p>
                  <small class="text-muted">Tamaño máximo total: 50MB.</small>
                </div>

                <transition-group name="list" tag="div" class="mt-3">
                  <b-list-group-item
                    v-for="(file, index) in existingFilesInfo"
                    :key="`existing-${index}`"
                    class="d-flex justify-content-between align-items-center"
                  >
                    <div class="d-flex justify-content-between align-items-center">
                      <font-awesome-icon icon="paperclip" class="text-muted" />

                      <b-button variant="link" class="p-0 text-left" @click="downloadAttachedFile(file.urlArchivo, file.nombre)">
                        {{ file.nombre }}
                      </b-button>
                    </div>
                    <b-button
                      variant="link"
                      class="p-0 text-danger"
                      @click="removeExistingFile(index)"
                      v-b-tooltip.hover
                      title="Eliminar archivo"
                    >
                      <font-awesome-icon icon="times-circle" />
                    </b-button>
                  </b-list-group-item>
                </transition-group>

                <transition-group name="list" tag="div" class="mt-1">
                  <b-list-group-item
                    v-for="(file, index) in files"
                    :key="file.name"
                    class="d-flex justify-content-between align-items-center"
                  >
                    <span>
                      <font-awesome-icon icon="paperclip" class="text-muted mr-2" />
                      {{ file.name }}
                    </span>
                    <b-button variant="link" class="p-0 text-danger" @click="removeFile(index)" v-b-tooltip.hover title="Eliminar archivo">
                      <font-awesome-icon icon="times-circle" />
                    </b-button>
                  </b-list-group-item>
                </transition-group>
              </b-form-group>

              <div class="d-flex justify-content-end mt-5 border-top pt-4">
                <b-button variant="secondary" class="mr-3" @click="previousState()" data-cy="entityCreateCancelButton">
                  <font-awesome-icon icon="ban" />
                  <span class="ml-1" v-text="t$('entity.action.cancel')"></span>
                </b-button>
                <b-button
                  variant="primary"
                  type="submit"
                  :disabled="v$.$invalid || isSaving || isUploading"
                  data-cy="entityCreateSaveButton"
                  style="min-width: 150px"
                >
                  <span v-if="isSaving">
                    <b-spinner small></b-spinner>
                    <span class="ml-2">Guardando...</span>
                  </span>
                  <span v-else>
                    <font-awesome-icon icon="save" />
                    <span class="ml-1" v-text="t$('entity.action.save')"></span>
                  </span>
                </b-button>
              </div>
            </b-form>
          </b-card-body>
        </b-card>
      </b-col>
    </b-row>
  </b-container>
</template>

<script lang="ts" src="./respuesta-update.component.ts"></script>

<style scoped>
.b-form-group {
  margin-bottom: 1.75rem;
}

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

.file-drop-zone.is-uploading {
  cursor: not-allowed;
  background-color: #e9ecef;
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
</style>
