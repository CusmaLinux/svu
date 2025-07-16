<template>
  <main>
    <b-container class="py-5">
      <b-row class="justify-content-center">
        <b-col md="8" lg="6">
          <b-card no-body class="shadow-sm border-0">
            <b-card-header class="bg-light py-3">
              <h2 class="text-center mb-0 h3 font-weight-bold">Consultar Estado de PQRSD</h2>
            </b-card-header>

            <b-card-body class="p-4">
              <b-form @submit.prevent="consult">
                <b-alert show variant="info" class="d-flex align-items-center mb-4">
                  <font-awesome-icon icon="circle-info" class="fa-2x mr-3" />
                  <div>Ingrese el número de radicado para verificar el estado actual de su solicitud.</div>
                </b-alert>

                <b-form-group label-for="numero_radicado" label-class="font-weight-bold">
                  <template #label> Número de Radicado <span class="text-danger">*</span> </template>

                  <b-input-group>
                    <b-input-group-prepend is-text>
                      <font-awesome-icon icon="hashtag" />
                    </b-input-group-prepend>
                    <b-form-input
                      id="numero_radicado"
                      v-model.trim="v$.fileNumber.$model"
                      type="text"
                      placeholder="Ej: R2025071500009"
                      :state="v$.fileNumber.$dirty ? !v$.fileNumber.$error : null"
                      data-cy="file-number"
                      required
                    ></b-form-input>
                  </b-input-group>

                  <b-form-invalid-feedback :state="!v$.fileNumber.$error" v-for="error of v$.fileNumber.$errors" :key="error.$uid">
                    {{ error.$message }}
                  </b-form-invalid-feedback>
                </b-form-group>

                <div class="d-flex justify-content-end mt-4 border-top pt-4">
                  <b-button variant="secondary" class="mr-3" @click="previousState">
                    <font-awesome-icon icon="arrow-left" class="mr-1" />
                    Volver
                  </b-button>
                  <b-button
                    variant="primary"
                    type="submit"
                    :disabled="v$.$invalid || isConsulting"
                    data-cy="consultButton"
                    style="min-width: 130px"
                  >
                    <span v-if="isConsulting">
                      <b-spinner small></b-spinner>
                      <span class="ml-2">Consultando...</span>
                    </span>
                    <span v-else>
                      <font-awesome-icon icon="search" class="mr-1" />
                      Consultar
                    </span>
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
