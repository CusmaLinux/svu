<template>
  <div class="row justify-content-center">
    <div class="col-md-8 col-lg-6">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2
          id="ventanillaUnicaApp.respuesta.home.createOrEditLabel"
          data-cy="RespuestaCreateUpdateHeading"
          v-text="t$('ventanillaUnicaApp.respuesta.home.createOrEditLabel')"
        ></h2>
        <div v-if="selectedPqrsInfo" class="alert alert-info mt-3" role="alert">
          <span v-if="!respuesta.id" v-text="t$('ventanillaUnicaApp.respuesta.respondingToPqrs')"></span>
          <span v-else v-text="t$('ventanillaUnicaApp.respuesta.associatedPqrs')"></span>:
          <strong>{{ selectedPqrsInfo.titulo || selectedPqrsInfo.id }}</strong>
        </div>

        <div>
          <div class="form-group" v-if="respuesta.id">
            <label for="id" v-text="t$('global.field.id')"></label>
            <input type="text" class="form-control" id="id" name="id" v-model="respuesta.id" readonly />
          </div>
          <div class="form-group" v-if="!respuesta.pqr && !isPqrsFixed">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.respuesta.pqr')" for="respuesta-pqr"></label>
            <select
              class="form-control"
              id="respuesta-pqr"
              data-cy="pqr"
              name="pqr"
              v-model="v$.pqr.$model"
              :class="{ valid: !v$.pqr.$invalid, invalid: v$.pqr.$invalid }"
              required
              :disabled="isPqrsFixed && !respuesta.id"
            >
              <option :value="null" v-text="t$('global.form.pleaseSelect')"></option>
              <option v-for="pqrsOption in allPqrs" :key="pqrsOption.id" :value="pqrsOption">
                {{ pqrsOption.titulo || pqrsOption.id }}
              </option>
            </select>
            <div v-if="v$.pqr.$anyDirty && v$.pqr.$invalid">
              <small class="form-text text-danger" v-for="error of v$.pqr.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.respuesta.contenido')" for="respuesta-contenido"></label>
            <textarea
              class="form-control"
              name="contenido"
              id="respuesta-contenido"
              data-cy="contenido"
              :class="{ valid: !v$.contenido.$invalid, invalid: v$.contenido.$invalid }"
              v-model="v$.contenido.$model"
              required
              rows="5"
            ></textarea>
            <div v-if="v$.contenido.$anyDirty && v$.contenido.$invalid">
              <small class="form-text text-danger" v-for="error of v$.contenido.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
        </div>
        <div>
          <input type="file" ref="fileInput" @change="onFileChange" multiple style="display: none" />
          <b-button class="mb-3" type="button" variant="primary" @click="triggerFileInput" :disabled="isUploading">
            {{ isUploading ? 'Subiendo...' : 'Seleccionar archivos' }}
          </b-button>
          <div class="mb-5">
            <b-list-group class="flex-wrap" horizontal v-if="existingFilesInfo?.length > 0">
              <b-list-group-item class="border" v-for="(file, index) in existingFilesInfo" :key="index">
                <b-badge class="text-wrap" variant="light"
                  ><b-button variant="link" @click="downloadAttachedFile(file.urlArchivo, file.nombre)">{{
                    file.nombre
                  }}</b-button></b-badge
                >
                <b-icon
                  class="cursor-pointer"
                  icon="x-square-fill"
                  variant="danger"
                  role="button"
                  @click="removeExistingFile(index)"
                ></b-icon>
              </b-list-group-item>
            </b-list-group>

            <b-list-group class="flex-wrap" horizontal v-if="files?.length > 0">
              <b-list-group-item class="border" v-for="(file, index) in files" :key="index">
                <b-badge class="text-wrap" variant="light">{{ file.name }}</b-badge>
                <b-icon class="cursor-pointer" icon="x-square-fill" variant="danger" role="button" @click="removeFile(index)"></b-icon>
              </b-list-group-item>
            </b-list-group>
          </div>
        </div>
        <div class="mt-4">
          <button type="button" id="cancel-save" data-cy="entityCreateCancelButton" class="btn btn-secondary" @click="previousState()">
            <font-awesome-icon icon="ban"></font-awesome-icon> <span v-text="t$('entity.action.cancel')"></span>
          </button>
          <button
            type="submit"
            id="save-entity"
            data-cy="entityCreateSaveButton"
            :disabled="v$.$invalid || isSaving"
            class="btn btn-primary ms-2"
          >
            <font-awesome-icon icon="save"></font-awesome-icon> <span v-text="t$('entity.action.save')"></span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
<script lang="ts" src="./respuesta-update.component.ts"></script>
