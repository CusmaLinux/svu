<template>
  <b-container class="py-5">
    <b-row class="justify-content-center">
      <b-col md="10" lg="8">
        <b-card no-body class="shadow-sm border-0">
          <b-card-header class="bg-light py-3">
            <h2 id="ventanillaUnicaApp.pqrs.public.title" data-cy="PublicPqrsCreate" class="text-center mb-0 h3 font-weight-bold">
              Registrar Petición, Queja, Reclamo o Sugerencia
            </h2>
          </b-card-header>

          <b-card-body>
            <b-form @submit.prevent="save">
              <b-alert show variant="info" class="d-flex align-items-center mb-4">
                <font-awesome-icon icon="circle-info" class="fa-2x mr-3" />
                <div>
                  Utilice este formulario para registrar su PQRSD. Le enviaremos el número de radicado y las actualizaciones a su correo
                  electrónico. Los campos con <span class="text-danger">*</span> son obligatorios.
                </div>
              </b-alert>

              <b-form-group
                id="requester-email-group"
                :label="t$('global.form[\'email.label\']')"
                label-for="requesterEmail"
                label-class="font-weight-bold"
              >
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <font-awesome-icon icon="at" />
                  </b-input-group-prepend>
                  <b-form-input
                    id="requesterEmail"
                    v-model="requesterEmailModel"
                    type="email"
                    :placeholder="t$('global.form[\'email.placeholder\']')"
                    :state="v$.requesterEmail.$dirty ? !v$.requesterEmail.$error : null"
                    data-cy="requesterEmail"
                  ></b-form-input>
                </b-input-group>
                <b-form-invalid-feedback :state="!v$.requesterEmail.$error" v-for="error of v$.requesterEmail.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group id="pqrs-type-group" label-for="pqrs-type" label-class="font-weight-bold">
                <template #label> {{ t$('ventanillaUnicaApp.pqrs.public.type') }} <span class="text-danger">*</span> </template>
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <font-awesome-icon icon="list-ul" />
                  </b-input-group-prepend>
                  <b-form-select
                    id="pqrs-type"
                    v-model="v$.type.$model"
                    :options="pqrsTypeOptions"
                    :state="v$.type.$dirty ? !v$.type.$error : null"
                    data-cy="PqrsType"
                  ></b-form-select>
                </b-input-group>
                <b-form-invalid-feedback :state="!v$.type.$error" v-for="error of v$.type.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group id="pqrs-titulo-group" label-for="pqrs-titulo" label-class="font-weight-bold">
                <template #label> {{ t$('ventanillaUnicaApp.pqrs.titulo') }} <span class="text-danger">*</span> </template>
                <b-input-group>
                  <b-input-group-prepend is-text>
                    <font-awesome-icon icon="tag" />
                  </b-input-group-prepend>
                  <b-form-input
                    id="pqrs-titulo"
                    v-model.trim="v$.titulo.$model"
                    :state="v$.titulo.$dirty ? !v$.titulo.$error : null"
                    required
                    data-cy="titulo"
                  ></b-form-input>
                </b-input-group>
                <b-form-invalid-feedback :state="!v$.titulo.$error" v-for="error of v$.titulo.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group id="pqrs-descripcion-group" label-for="pqrs-descripcion" label-class="font-weight-bold">
                <template #label> {{ t$('ventanillaUnicaApp.pqrs.descripcion') }} <span class="text-danger">*</span> </template>
                <b-form-textarea
                  id="pqrs-descripcion"
                  v-model.trim="v$.descripcion.$model"
                  rows="5"
                  placeholder="Describa su solicitud de la forma más detallada posible..."
                  :state="v$.descripcion.$dirty ? !v$.descripcion.$error : null"
                  required
                  data-cy="descripcion"
                ></b-form-textarea>
                <b-form-invalid-feedback :state="!v$.descripcion.$error" v-for="error of v$.descripcion.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group
                :label-class="['font-weight-bold']"
                :label="t$('ventanillaUnicaApp.pqrs.archivosAdjuntos', 'Archivos adjuntos')"
              >
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
                <b-button variant="secondary" class="mr-3" @click="previousState()">
                  <font-awesome-icon icon="ban" />
                  <span class="ml-1">Cancelar</span>
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
                    <span class="ml-2">Enviando...</span>
                  </span>
                  <span v-else>
                    <font-awesome-icon icon="paper-plane" />
                    <span class="ml-1">Enviar PQRSD</span>
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

<script lang="ts" src="./public-pqrs-create.component.ts"></script>

<style scoped>
/* More spacing between form groups for better readability */
.b-form-group {
  margin-bottom: 1.75rem;
}

/* Styling for the drag-and-drop zone */
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

/* Animations for the file list */
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
