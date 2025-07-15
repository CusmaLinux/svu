<template>
  <b-container class="py-5">
    <b-row class="justify-content-center">
      <b-col md="10" lg="8">
        <b-card>
          <b-form @submit.prevent="save">
            <h2 id="ventanillaUnicaApp.pqrs.public.title" data-cy="PublicPqrsCreate" class="text-center mb-4">
              Crear una Petición, Queja, Reclamo o Sugerencia
            </h2>

            <b-alert show variant="info" class="mb-4">
              <h4 class="alert-heading">¡Bienvenido!</h4>
              <p>
                Utilice este formulario para registrar su PQRSD. Por favor, proporcione un correo electrónico válido para que podamos
                enviarle el número de radicado y las actualizaciones de su caso.
              </p>
              <hr />
              <p class="mb-0">Todos los campos marcados con <span class="text-danger">*</span> son obligatorios.</p>
            </b-alert>

            <b-form-group
              id="requester-email-group"
              :label="t$('global.form[\'email.label\']')"
              label-for="requesterEmail"
              :description="t$('ventanillaUnicaApp.pqrs.public.requesterEmail.info')"
            >
              <b-form-input
                id="requesterEmail"
                v-model="requesterEmailModel"
                type="email"
                :placeholder="t$('global.form[\'email.placeholder\']')"
                :state="v$.requesterEmail.$dirty ? !v$.requesterEmail.$error : null"
                data-cy="requesterEmail"
              ></b-form-input>
              <b-form-invalid-feedback :state="!v$.requesterEmail.$error" v-for="error of v$.requesterEmail.$errors" :key="error.$uid">
                {{ error.$message }}
              </b-form-invalid-feedback>
            </b-form-group>

            <b-form-group id="pqrs-type-group" :label="t$('ventanillaUnicaApp.pqrs.public.type') + ' *'" label-for="pqrs-type">
              <b-form-select
                id="pqrs-type"
                v-model="v$.type.$model"
                :options="pqrsTypeOptions"
                :state="v$.type.$dirty ? !v$.type.$error : null"
                data-cy="PqrsType"
              ></b-form-select>
              <b-form-invalid-feedback :state="!v$.type.$error" v-for="error of v$.type.$errors" :key="error.$uid">
                {{ error.$message }}
              </b-form-invalid-feedback>
            </b-form-group>

            <b-form-group id="pqrs-titulo-group" :label="t$('ventanillaUnicaApp.pqrs.titulo') + ' *'" label-for="pqrs-titulo">
              <b-form-input
                id="pqrs-titulo"
                v-model.trim="v$.titulo.$model"
                :state="v$.titulo.$dirty ? !v$.titulo.$error : null"
                required
                data-cy="titulo"
              ></b-form-input>
              <b-form-invalid-feedback :state="!v$.titulo.$error" v-for="error of v$.titulo.$errors" :key="error.$uid">
                {{ error.$message }}
              </b-form-invalid-feedback>
            </b-form-group>

            <b-form-group
              id="pqrs-descripcion-group"
              :label="t$('ventanillaUnicaApp.pqrs.descripcion') + ' *'"
              label-for="pqrs-descripcion"
            >
              <b-form-textarea
                id="pqrs-descripcion"
                v-model.trim="v$.descripcion.$model"
                rows="5"
                :state="v$.descripcion.$dirty ? !v$.descripcion.$error : null"
                required
                data-cy="descripcion"
              ></b-form-textarea>
              <b-form-invalid-feedback :state="!v$.descripcion.$error" v-for="error of v$.descripcion.$errors" :key="error.$uid">
                {{ error.$message }}
              </b-form-invalid-feedback>
            </b-form-group>

            <b-form-group :label="t$('ventanillaUnicaApp.pqrs.archivosAdjuntos', 'Archivos adjuntos')">
              <input type="file" ref="fileInput" @change="onFileChange" multiple style="display: none" />
              <b-button @click="triggerFileInput" variant="outline-primary" :disabled="isUploading">
                <font-awesome-icon icon="upload" class="mr-1" />
                {{ isUploading ? 'Subiendo...' : 'Adjuntar archivos' }}
              </b-button>
              <small class="form-text text-muted mt-2">
                Puede adjuntar cualquier cantidad de archivos (PDF, DOCX, JPG, etc.). Tamaño máximo por todos los archivo: 50MB.
              </small>

              <b-list-group class="mt-3" v-if="hasFiles">
                <b-list-group-item
                  v-for="(file, index) in files"
                  :key="`new-${index}`"
                  class="d-flex justify-content-between align-items-center"
                >
                  <span>
                    <font-awesome-icon icon="paperclip" class="text-muted mr-2" />
                    {{ file.name }}
                  </span>
                  <b-button variant="link" class="p-0 text-danger" @click="removeFile(index)" v-b-tooltip.hover title="Eliminar archivo">
                    <font-awesome-icon icon="trash-alt" />
                  </b-button>
                </b-list-group-item>
              </b-list-group>
            </b-form-group>

            <div class="d-flex justify-content-end mt-5">
              <b-button variant="secondary" class="mr-2" @click="previousState()">
                <font-awesome-icon icon="ban" />
                <span class="ml-1">Cancelar</span>
              </b-button>
              <b-button variant="primary" type="submit" :disabled="v$.$invalid || isSaving || isUploading" data-cy="entityCreateSaveButton">
                <font-awesome-icon icon="save" />
                <span class="ml-1">Guardar</span>
              </b-button>
            </div>
          </b-form>
        </b-card>
      </b-col>
    </b-row>
  </b-container>
</template>

<script lang="ts" src="./public-pqrs-create.component.ts"></script>

<style scoped>
.b-form-group {
  margin-bottom: 1.5rem;
}
</style>
