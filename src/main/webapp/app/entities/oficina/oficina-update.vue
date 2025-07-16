<template>
  <b-container>
    <div class="row justify-content-center">
      <div class="col-lg-8">
        <b-card no-body class="shadow-sm border-0">
          <b-card-header class="bg-light py-3">
            <h2
              id="ventanillaUnicaApp.oficina.home.createOrEditLabel"
              data-cy="OficinaCreateUpdateHeading"
              class="text-center mb-0 h3 font-weight-bold"
            >
              {{ oficina.id ? 'Editar Oficina' : 'Crear Nueva Oficina' }}
            </h2>
          </b-card-header>

          <b-card-body class="p-4">
            <b-form @submit.prevent="save()">
              <b-alert show variant="primary" class="d-flex align-items-center mb-4">
                <font-awesome-icon icon="building" class="fa-2x mr-3" />
                <div>Gestione los detalles de la oficina. Asignar una oficina superior ayuda a construir la jerarquía organizacional.</div>
              </b-alert>

              <b-form-group v-if="oficina.id" :label="t$('global.field.id')" label-for="id" label-class="font-weight-bold">
                <b-form-input id="id" v-model="oficina.id" readonly></b-form-input>
              </b-form-group>

              <b-row>
                <b-col md="6">
                  <b-form-group label-for="oficina-nombre" label-class="font-weight-bold">
                    <template #label> Nombre de la Oficina <span class="text-danger">*</span> </template>
                    <b-input-group>
                      <b-input-group-prepend is-text><font-awesome-icon icon="tag" /></b-input-group-prepend>
                      <b-form-input
                        id="oficina-nombre"
                        v-model="v$.nombre.$model"
                        :state="v$.nombre.$dirty ? !v$.nombre.$error : null"
                        data-cy="nombre"
                      ></b-form-input>
                    </b-input-group>
                    <b-form-invalid-feedback :state="!v$.nombre.$error" v-for="error of v$.nombre.$errors" :key="error.$uid">
                      {{ error.$message }}
                    </b-form-invalid-feedback>
                  </b-form-group>
                </b-col>
                <b-col md="6">
                  <b-form-group label-for="oficina-nivel" label-class="font-weight-bold">
                    <template #label> Nivel (Jerarquía) <span class="text-danger">*</span> </template>
                    <b-input-group>
                      <b-input-group-prepend is-text><font-awesome-icon icon="layer-group" /></b-input-group-prepend>
                      <b-form-input
                        id="oficina-nivel"
                        v-model="v$.nivel.$model"
                        :state="v$.nivel.$dirty ? !v$.nivel.$error : null"
                        data-cy="nivel"
                      ></b-form-input>
                    </b-input-group>
                    <b-form-invalid-feedback :state="!v$.nivel.$error" v-for="error of v$.nivel.$errors" :key="error.$uid">
                      {{ error.$message }}
                    </b-form-invalid-feedback>
                  </b-form-group>
                </b-col>
              </b-row>

              <b-form-group label="Descripción" label-for="oficina-descripcion" label-class="font-weight-bold">
                <b-input-group>
                  <b-input-group-prepend is-text><font-awesome-icon icon="align-left" /></b-input-group-prepend>
                  <b-form-input
                    id="oficina-descripcion"
                    v-model="v$.descripcion.$model"
                    :state="v$.descripcion.$dirty ? !v$.descripcion.$error : null"
                    data-cy="descripcion"
                  ></b-form-input>
                </b-input-group>
              </b-form-group>

              <b-form-group label="Oficina Superior (Padre)" label-for="oficina-oficinaSuperior" label-class="font-weight-bold">
                <b-input-group>
                  <b-input-group-prepend is-text><font-awesome-icon icon="sitemap" /></b-input-group-prepend>
                  <b-form-input
                    id="oficina-oficinaSuperior"
                    v-model="v$.oficinaSuperior.$model"
                    :state="v$.oficinaSuperior.$dirty ? !v$.oficinaSuperior.$error : null"
                    data-cy="oficinaSuperior"
                  ></b-form-input>
                </b-input-group>
              </b-form-group>

              <b-form-group label-class="font-weight-bold">
                <template #label> Usuario Responsable <span class="text-danger">*</span> </template>
                <v-multiselect
                  id="office-responsible"
                  data-cy="responsible"
                  :options="users"
                  label="login"
                  track-by="id"
                  :model-value="v$.responsableDTO.$model"
                  @update:model-value="v$.responsableDTO.$model = $event"
                  :class="{ 'is-invalid': v$.responsableDTO.$error && v$.responsableDTO.$anyDirty }"
                  placeholder="Seleccione un usuario"
                  select-label="Presione enter para seleccionar"
                  deselect-label="Presione enter para remover"
                  selected-label="Seleccionado"
                >
                  <template #noResult>No se encontraron resultados</template>
                  <template #noOptions>La lista está vacía</template>
                </v-multiselect>
                <b-form-invalid-feedback :state="!v$.responsableDTO.$error" v-for="error of v$.responsableDTO.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <div class="d-flex justify-content-end mt-5 border-top pt-4">
                <b-button variant="secondary" class="mr-3" @click="previousState()">
                  <font-awesome-icon icon="ban" />
                  <span class="ml-1" v-text="t$('entity.action.cancel')"></span>
                </b-button>
                <b-button
                  variant="primary"
                  type="submit"
                  :disabled="v$.$invalid || isSaving"
                  data-cy="entityCreateSaveButton"
                  style="min-width: 120px"
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
      </div>
    </div>
  </b-container>
</template>

<script lang="ts" src="./oficina-update.component.ts"></script>

<style>
.multiselect.is-invalid .multiselect__tags {
  border-color: #dc3545;
}
</style>
