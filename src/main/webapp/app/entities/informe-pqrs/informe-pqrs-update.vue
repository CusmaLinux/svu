<template>
  <b-container>
    <div class="row justify-content-center">
      <div class="col-lg-8">
        <b-card no-body class="shadow-sm border-0">
          <b-card-header class="bg-light py-3">
            <h2
              id="ventanillaUnicaApp.informePqrs.home.createOrEditLabel"
              data-cy="InformePqrsCreateUpdateHeading"
              class="text-center mb-0 h3 font-weight-bold"
            >
              {{ informePqrs.id ? 'Editar Informe PQRS' : 'Crear Informe PQRS' }}
            </h2>
          </b-card-header>

          <b-card-body class="p-4">
            <b-form @submit.prevent="save()">
              <b-alert show variant="info" class="d-flex align-items-center mb-4">
                <font-awesome-icon icon="info-circle" class="fa-2x mr-3" />
                <div>Complete los campos de fechas y asigne la oficina correspondiente para el informe.</div>
              </b-alert>

              <b-form-group v-if="informePqrs.id" :label="t$('global.field.id')" label-for="id" label-class="font-weight-bold">
                <b-form-input id="id" v-model="informePqrs.id" readonly></b-form-input>
              </b-form-group>

              <b-form-group label-class="font-weight-bold">
                <template #label> {{ t$('ventanillaUnicaApp.informePqrs.fechaInicio') }} <span class="text-danger">*</span> </template>
                <b-input-group>
                  <b-input-group-prepend is-text><font-awesome-icon icon="calendar-alt" /></b-input-group-prepend>
                  <b-form-input
                    id="informe-pqrs-fechaInicio"
                    data-cy="fechaInicio"
                    type="datetime-local"
                    :state="v$.fechaInicio.$dirty ? !v$.fechaInicio.$error : null"
                    :value="convertDateTimeFromServer(v$.fechaInicio.$model)"
                    @change="updateInstantField('fechaInicio', $event)"
                  ></b-form-input>
                </b-input-group>
                <b-form-invalid-feedback :state="!v$.fechaInicio.$error" v-for="error of v$.fechaInicio.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group label-class="font-weight-bold">
                <template #label> {{ t$('ventanillaUnicaApp.informePqrs.fechaFin') }} <span class="text-danger">*</span> </template>
                <b-input-group>
                  <b-input-group-prepend is-text><font-awesome-icon icon="calendar-check" /></b-input-group-prepend>
                  <b-form-input
                    id="informe-pqrs-fechaFin"
                    data-cy="fechaFin"
                    type="datetime-local"
                    :state="v$.fechaFin.$dirty ? !v$.fechaFin.$error : null"
                    :value="convertDateTimeFromServer(v$.fechaFin.$model)"
                    @change="updateInstantField('fechaFin', $event)"
                  ></b-form-input>
                </b-input-group>
                <b-form-invalid-feedback :state="!v$.fechaFin.$error" v-for="error of v$.fechaFin.$errors" :key="error.$uid">
                  {{ error.$message }}
                </b-form-invalid-feedback>
              </b-form-group>

              <b-form-group label-class="font-weight-bold">
                <template #label> {{ t$('ventanillaUnicaApp.informePqrs.oficina') }} </template>
                <div class="d-flex">
                  <div class="input-group-prepend">
                    <span class="input-group-text"><font-awesome-icon icon="building" /></span>
                  </div>
                  <div class="flex-grow-1">
                    <v-multiselect
                      id="informe-pqrs-oficina"
                      data-cy="oficina"
                      :options="oficinas"
                      label="nombre"
                      track-by="id"
                      :model-value="v$.oficina.$model"
                      @update:model-value="v$.oficina.$model = $event"
                      :class="{ 'is-invalid': v$.oficina.$error && v$.oficina.$anyDirty }"
                      placeholder="Seleccione una oficina"
                      select-label="Presione enter para seleccionar"
                      deselect-label="Presione enter para remover"
                      selected-label="Seleccionado"
                    >
                      <template #noResult>No se encontraron resultados</template>
                      <template #noOptions>La lista está vacía</template>
                    </v-multiselect>
                  </div>
                </div>
                <b-form-invalid-feedback :state="!v$.oficina.$error" v-for="error of v$.oficina.$errors" :key="error.$uid">
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

<script lang="ts" src="./informe-pqrs-update.component.ts"></script>

<style>
.multiselect.is-invalid .multiselect__tags {
  border-color: #dc3545;
}

.input-group-prepend + div .multiselect__tags {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}
</style>
