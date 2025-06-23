<template>
  <main>
    <b-container class="py-5">
      <b-row class="justify-content-center">
        <b-col md="10" lg="8">
          <b-card no-body>
            <b-card-body class="p-4 p-md-5">
              <h1 class="h3 font-weight-bold text-dark mb-4 text-center">Consultar Estado de su Requerimiento</h1>

              <b-alert show variant="info" class="d-flex align-items-center mb-4" role="alert">
                <font-awesome-icon icon="info-circle" class="mr-3" style="font-size: 1.5rem" />
                <p class="mb-0 small">
                  Ingrese el número de radicado de su PQRSD (Petición, Queja, Reclamo, Sugerencia o Denuncia) para verificar su estado
                  actual.
                </p>
              </b-alert>

              <b-form name="consultForm" no-validate>
                <b-form-group>
                  <template #label> Número de Radicado <span class="text-danger">*</span> </template>

                  <b-input-group class="d-flex flex-row">
                    <b-input-group-prepend is-text>
                      <font-awesome-icon icon="file-lines" class="text-secondary" />
                    </b-input-group-prepend>
                    <b-form-input
                      id="numero_radicado"
                      name="numero_radicado"
                      type="text"
                      data-cy="file-number"
                      placeholder="Ingrese su número de radicado"
                      :class="{ valid: !v$.fileNumber.$invalid, invalid: v$.fileNumber.$invalid }"
                      v-model="v$.fileNumber.$model"
                      required
                    >
                    </b-form-input>
                  </b-input-group>
                  <div v-if="v$.fileNumber.$anyDirty && v$.fileNumber.$invalid">
                    <small class="form-text text-danger" v-for="error of v$.fileNumber.$errors" :key="error.$uid">{{
                      error.$message
                    }}</small>
                  </div>
                </b-form-group>

                <div class="d-flex justify-content-end mt-4">
                  <b-button variant="secondary" type="button" class="mr-3 br-lg" @click="previousState">
                    <font-awesome-icon icon="ban" class="mr-2" />
                    Cancelar
                  </b-button>
                  <b-button variant="primary" type="submit" class="br-lg" :disabled="v$.$invalid || isConsulting" @click="consult">
                    <font-awesome-icon icon="search" class="mr-2" />
                    Consultar
                  </b-button>
                </div>
              </b-form>
            </b-card-body>
          </b-card>
        </b-col>
      </b-row>
    </b-container>
  </main>
</template>

<script src="./consult-requirement.component.ts" lang="ts"></script>

<style scoped>
.h3 {
  font-size: 1.75rem;
}
</style>
