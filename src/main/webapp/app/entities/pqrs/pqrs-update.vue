<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2
          id="ventanillaUnicaApp.pqrs.home.createOrEditLabel"
          data-cy="PqrsCreateUpdateHeading"
          v-text="t$('ventanillaUnicaApp.pqrs.home.createOrEditLabel')"
        ></h2>
        <div>
          <div class="form-group" v-if="pqrs.id">
            <label for="id" v-text="t$('global.field.id')"></label>
            <input type="text" class="form-control" id="id" name="id" v-model="pqrs.id" readonly />
          </div>

          <div class="form-group">
            <label class="form-control-label" for="requesterEmail" v-text="t$('global.form[\'email.label\']')"></label>
            <font-awesome-icon
              class="cursor-pointer"
              v-b-tooltip.hover.top="t$('ventanillaUnicaApp.pqrs.public.requesterEmail.info')"
              variant="primary"
              icon="circle-info"
            />
            <input
              type="email"
              class="form-control"
              id="requesterEmail"
              name="requesterEmail"
              :class="{ valid: !v$.requesterEmail.$invalid, invalid: v$.requesterEmail.$invalid }"
              v-model="requesterEmailModel"
              minlength="5"
              maxlength="254"
              email
              :placeholder="t$('global.form[\'email.placeholder\']')"
              data-cy="requesterEmail"
            />
            <div v-if="v$.requesterEmail.$anyDirty && v$.requesterEmail.$invalid">
              <small class="form-text text-danger" v-for="error of v$.requesterEmail.$errors" :key="error.$uid">
                {{ error.$message }}
              </small>
            </div>
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.pqrs.public.type')" for="pqrs-type"></label>
            <select
              class="form-control"
              name="type"
              id="pqrs-type"
              data-cy="PqrsType"
              :class="{ valid: !v$.type.$invalid, invalid: v$.type.$invalid }"
              v-model="v$.type.$model"
            >
              <option :value="null" disabled>-- Seleccione un tipo --</option>
              <option v-for="(enumValue, enumKey) in PqrsType" :key="enumKey" :value="enumValue">
                {{ enumValue }}
              </option>
            </select>
            <div v-if="v$.type.$anyDirty && v$.type.$invalid">
              <small class="form-text text-danger" v-for="error of v$.type.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.pqrs.titulo')" for="pqrs-titulo"></label>
            <input
              type="text"
              class="form-control"
              name="titulo"
              id="pqrs-titulo"
              data-cy="titulo"
              :class="{ valid: !v$.titulo.$invalid, invalid: v$.titulo.$invalid }"
              v-model="v$.titulo.$model"
              required
            />
            <div v-if="v$.titulo.$anyDirty && v$.titulo.$invalid">
              <small class="form-text text-danger" v-for="error of v$.titulo.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.pqrs.descripcion')" for="pqrs-descripcion"></label>
            <textarea
              class="form-control"
              name="descripcion"
              id="pqrs-descripcion"
              data-cy="descripcion"
              :class="{ valid: !v$.descripcion.$invalid, invalid: v$.descripcion.$invalid }"
              v-model="v$.descripcion.$model"
              required
            ></textarea>
            <div v-if="v$.descripcion.$anyDirty && v$.descripcion.$invalid">
              <small class="form-text text-danger" v-for="error of v$.descripcion.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div v-if="isUpdateMode" class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.pqrs.fechaCreacion')" for="pqrs-fechaCreacion"></label>
            <div class="d-flex">
              <input
                id="pqrs-fechaCreacion"
                data-cy="fechaCreacion"
                type="datetime-local"
                class="form-control"
                name="fechaCreacion"
                :class="{ valid: !v$.fechaCreacion.$invalid, invalid: v$.fechaCreacion.$invalid }"
                :value="convertDateTimeFromServer(v$.fechaCreacion.$model)"
              />
            </div>
            <div v-if="v$.fechaCreacion.$anyDirty && v$.fechaCreacion.$invalid">
              <small class="form-text text-danger" v-for="error of v$.fechaCreacion.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div v-if="isUpdateMode" v-can="['assign_due_date', 'pqrs']" class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.pqrs.daysForResponse')" for="pqrs-daysForResponse"></label>
            <input
              type="number"
              class="form-control"
              name="daysToReply"
              id="pqrs-daysToReply"
              data-cy="daysToReply"
              v-model="v$.daysToReply.$model"
              min="0"
              max="365"
              placeholder="15"
            />
          </div>
          <div v-if="isUpdateMode" v-can="['assign_due_date', 'pqrs']" class="form-group">
            <label
              class="form-control-label"
              v-text="t$('ventanillaUnicaApp.pqrs.fechaLimiteRespuesta')"
              for="pqrs-fechaLimiteRespuesta"
            ></label>
            <div class="d-flex">
              <input
                id="pqrs-fechaLimiteRespuesta"
                data-cy="fechaLimiteRespuesta"
                type="datetime-local"
                class="form-control"
                name="fechaLimiteRespuesta"
                :class="{ valid: !v$.fechaLimiteRespuesta.$invalid, invalid: v$.fechaLimiteRespuesta.$invalid }"
                :value="convertDateTimeFromServer(v$.fechaLimiteRespuesta.$model)"
                @change="updateDueDate($event)"
              />
            </div>
          </div>
          <div v-if="isUpdateMode" class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.pqrs.estado')" for="pqrs-estado"></label>
            <select
              class="form-control"
              name="estado"
              id="pqrs-estado"
              data-cy="estado"
              :class="{ valid: !v$.estado.$invalid, invalid: v$.estado.$invalid }"
              v-model="v$.estado.$model"
              :disabled="pqrs.estado === statesPqrs.Closed"
            >
              <option v-for="(enumValue, enumKey) in statesPqrs" :key="enumKey" :value="enumValue">
                {{ enumValue }}
              </option>
            </select>
            <div v-if="v$.estado.$anyDirty && v$.estado.$invalid">
              <small class="form-text text-danger" v-for="error of v$.estado.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>

          <div v-if="isUpdateMode" class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.pqrs.oficinaResponder')" for="pqrs-oficinaResponder"></label>
            <select
              class="form-control"
              id="pqrs-oficinaResponder"
              data-cy="oficinaResponder"
              name="oficinaResponder"
              v-model="pqrs.oficinaResponder"
            >
              <option
                v-for="oficinaOption in oficinas"
                :value="pqrs.oficinaResponder && oficinaOption.id === pqrs.oficinaResponder.id ? pqrs.oficinaResponder : oficinaOption"
                :key="oficinaOption.id"
              >
                {{ oficinaOption.nombre }}
              </option>
            </select>
          </div>
        </div>
        <div>
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
          <button type="button" id="cancel-save" data-cy="entityCreateCancelButton" class="btn btn-secondary" @click="previousState()">
            <font-awesome-icon icon="ban"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.cancel')"></span>
          </button>
          <button
            type="button"
            id="save-entity"
            @click="save"
            data-cy="entityCreateSaveButton"
            :disabled="v$.$invalid || isSaving || isUploading"
            class="btn btn-primary"
          >
            <font-awesome-icon icon="save"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.save')"></span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
<script lang="ts" src="./pqrs-update.component.ts"></script>
