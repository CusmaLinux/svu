<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2
          id="ventanillaUnicaApp.respuesta.home.createOrEditLabel"
          data-cy="RespuestaCreateUpdateHeading"
          v-text="t$('ventanillaUnicaApp.respuesta.home.createOrEditLabel')"
        ></h2>
        <div>
          <div class="form-group" v-if="respuesta.id">
            <label for="id" v-text="t$('global.field.id')"></label>
            <input type="text" class="form-control" id="id" name="id" v-model="respuesta.id" readonly />
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
            ></textarea>
            <div v-if="v$.contenido.$anyDirty && v$.contenido.$invalid">
              <small class="form-text text-danger" v-for="error of v$.contenido.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="form-group">
            <label
              class="form-control-label"
              v-text="t$('ventanillaUnicaApp.respuesta.fechaRespuesta')"
              for="respuesta-fechaRespuesta"
            ></label>
            <div class="d-flex">
              <input
                id="respuesta-fechaRespuesta"
                data-cy="fechaRespuesta"
                type="datetime-local"
                class="form-control"
                name="fechaRespuesta"
                :class="{ valid: !v$.fechaRespuesta.$invalid, invalid: v$.fechaRespuesta.$invalid }"
                required
                :value="convertDateTimeFromServer(v$.fechaRespuesta.$model)"
                @change="updateInstantField('fechaRespuesta', $event)"
              />
            </div>
            <div v-if="v$.fechaRespuesta.$anyDirty && v$.fechaRespuesta.$invalid">
              <small class="form-text text-danger" v-for="error of v$.fechaRespuesta.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.respuesta.pqr')" for="respuesta-pqr"></label>
            <select class="form-control" id="respuesta-pqr" data-cy="pqr" name="pqr" v-model="respuesta.pqr">
              <option :value="null"></option>
              <option
                :value="respuesta.pqr && pqrsOption.id === respuesta.pqr.id ? respuesta.pqr : pqrsOption"
                v-for="pqrsOption in pqrs"
                :key="pqrsOption.id"
              >
                {{ pqrsOption.id }}
              </option>
            </select>
          </div>
        </div>
        <div>
          <button type="button" id="cancel-save" data-cy="entityCreateCancelButton" class="btn btn-secondary" @click="previousState()">
            <font-awesome-icon icon="ban"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.cancel')"></span>
          </button>
          <button
            type="submit"
            id="save-entity"
            data-cy="entityCreateSaveButton"
            :disabled="v$.$invalid || isSaving"
            class="btn btn-primary"
          >
            <font-awesome-icon icon="save"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.save')"></span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
<script lang="ts" src="./respuesta-update.component.ts"></script>
