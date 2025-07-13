<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <form name="editForm" novalidate @submit.prevent="save()">
        <h2
          id="ventanillaUnicaApp.oficina.home.createOrEditLabel"
          data-cy="OficinaCreateUpdateHeading"
          v-text="t$('ventanillaUnicaApp.oficina.home.createOrEditLabel')"
        ></h2>
        <div>
          <div class="form-group" v-if="oficina.id">
            <label for="id" v-text="t$('global.field.id')"></label>
            <input type="text" class="form-control" id="id" name="id" v-model="oficina.id" readonly />
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.oficina.nombre')" for="oficina-nombre"></label>
            <input
              type="text"
              class="form-control"
              name="nombre"
              id="oficina-nombre"
              data-cy="nombre"
              :class="{ valid: !v$.nombre.$invalid, invalid: v$.nombre.$invalid }"
              v-model="v$.nombre.$model"
              required
            />
            <div v-if="v$.nombre.$anyDirty && v$.nombre.$invalid">
              <small class="form-text text-danger" v-for="error of v$.nombre.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.oficina.descripcion')" for="oficina-descripcion"></label>
            <input
              type="text"
              class="form-control"
              name="descripcion"
              id="oficina-descripcion"
              data-cy="descripcion"
              :class="{ valid: !v$.descripcion.$invalid, invalid: v$.descripcion.$invalid }"
              v-model="v$.descripcion.$model"
            />
          </div>
          <div class="form-group">
            <label class="form-control-label" v-text="t$('ventanillaUnicaApp.oficina.nivel')" for="oficina-nivel"></label>
            <input
              type="text"
              class="form-control"
              name="nivel"
              id="oficina-nivel"
              data-cy="nivel"
              :class="{ valid: !v$.nivel.$invalid, invalid: v$.nivel.$invalid }"
              v-model="v$.nivel.$model"
              required
            />
            <div v-if="v$.nivel.$anyDirty && v$.nivel.$invalid">
              <small class="form-text text-danger" v-for="error of v$.nivel.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
          </div>
          <div class="form-group">
            <label
              class="form-control-label"
              v-text="t$('ventanillaUnicaApp.oficina.oficinaSuperior')"
              for="oficina-oficinaSuperior"
            ></label>
            <input
              type="text"
              class="form-control"
              name="oficinaSuperior"
              id="oficina-oficinaSuperior"
              data-cy="oficinaSuperior"
              :class="{ valid: !v$.oficinaSuperior.$invalid, invalid: v$.oficinaSuperior.$invalid }"
              v-model="v$.oficinaSuperior.$model"
            />
          </div>
          <div class="form-group">
            <label class="form-control-label">Seleccione el usuario encargado</label>
            <v-multiselect
              id="office-responsible"
              data-cy="responsible"
              :options="users"
              label="login"
              track-by="id"
              :model-value="v$.responsableDTO.$model"
              @update:model-value="v$.responsableDTO.$model = $event"
              :class="{ 'is-invalid': v$.responsableDTO.$error }"
              placeholder="Seleccione un usuario"
              select-label="Presione enter para seleccionar"
              deselect-label="Presione enter para remover"
              selected-label="Seleccionado"
            >
              <template #noResult> No se encontraron resultados </template>
              <template #noOptions> La lista esta vacia </template>
            </v-multiselect>
            <div v-if="v$.responsableDTO.$anyDirty && v$.responsableDTO.$invalid">
              <small class="form-text text-danger" v-for="error of v$.responsableDTO.$errors" :key="error.$uid">{{ error.$message }}</small>
            </div>
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
<script lang="ts" src="./oficina-update.component.ts"></script>
