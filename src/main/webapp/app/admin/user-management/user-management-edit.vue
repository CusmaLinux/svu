<template>
  <b-container>
    <div class="row justify-content-center">
      <div class="col-lg-9">
        <b-card no-body class="shadow-sm border-0">
          <b-card-header class="bg-light py-3">
            <h2 id="myUserLabel" class="text-center mb-0 h3 font-weight-bold">
              {{ userAccount.id ? 'Editar Usuario' : 'Crear Nuevo Usuario' }}
            </h2>
          </b-card-header>

          <b-card-body class="p-4" v-if="userAccount">
            <b-form name="editForm" @submit.prevent="save()">
              <b-alert show variant="primary" class="d-flex align-items-center mb-4">
                <font-awesome-icon icon="user-cog" class="fa-2x mr-3" />
                <div>Administre los detalles del usuario, active o desactive su cuenta y asigne los perfiles correspondientes.</div>
              </b-alert>

              <h4 class="mb-3 text-secondary">Información del Usuario</h4>

              <b-form-group v-if="userAccount.id" :label="t$('global.field.id')" label-class="font-weight-bold">
                <b-form-input :value="userAccount.id" readonly></b-form-input>
              </b-form-group>

              <b-row>
                <b-col md="6">
                  <b-form-group label-class="font-weight-bold">
                    <template #label> Nombre de Usuario (Login) <span class="text-danger">*</span> </template>
                    <b-input-group>
                      <b-input-group-prepend is-text><font-awesome-icon icon="user" /></b-input-group-prepend>
                      <b-form-input
                        v-model="v$.userAccount.login.$model"
                        :state="v$.userAccount.login.$dirty ? !v$.userAccount.login.$error : null"
                      ></b-form-input>
                    </b-input-group>
                    <b-form-invalid-feedback
                      :state="!v$.userAccount.login.$error"
                      v-for="error of v$.userAccount.login.$errors"
                      :key="error.$uid"
                    >
                      {{ error.$message }}
                    </b-form-invalid-feedback>
                  </b-form-group>
                </b-col>
                <b-col md="6">
                  <b-form-group label-class="font-weight-bold">
                    <template #label> Correo Electrónico <span class="text-danger">*</span> </template>
                    <b-input-group>
                      <b-input-group-prepend is-text><font-awesome-icon icon="at" /></b-input-group-prepend>
                      <b-form-input
                        v-model="v$.userAccount.email.$model"
                        type="email"
                        :state="v$.userAccount.email.$dirty ? !v$.userAccount.email.$error : null"
                      ></b-form-input>
                    </b-input-group>
                    <b-form-invalid-feedback
                      :state="!v$.userAccount.email.$error"
                      v-for="error of v$.userAccount.email.$errors"
                      :key="error.$uid"
                    >
                      {{ error.$message }}
                    </b-form-invalid-feedback>
                  </b-form-group>
                </b-col>
              </b-row>

              <b-row>
                <b-col md="6">
                  <b-form-group :label="t$('userManagement.firstName')" label-class="font-weight-bold">
                    <b-form-input
                      v-model="v$.userAccount.firstName.$model"
                      :state="v$.userAccount.firstName.$dirty ? !v$.userAccount.firstName.$error : null"
                    ></b-form-input>
                  </b-form-group>
                </b-col>
                <b-col md="6">
                  <b-form-group :label="t$('userManagement.lastName')" label-class="font-weight-bold">
                    <b-form-input
                      v-model="v$.userAccount.lastName.$model"
                      :state="v$.userAccount.lastName.$dirty ? !v$.userAccount.lastName.$error : null"
                    ></b-form-input>
                  </b-form-group>
                </b-col>
              </b-row>

              <hr class="my-4" />
              <h4 class="mb-3 text-secondary">Configuración y Permisos</h4>

              <b-form-group>
                <b-form-checkbox
                  id="activated"
                  v-model="userAccount.activated"
                  :disabled="userAccount.id === null"
                  name="activated"
                  switch
                  size="lg"
                >
                  Cuenta Activada
                </b-form-checkbox>
              </b-form-group>

              <b-row>
                <b-col md="6">
                  <b-form-group v-if="languages && Object.keys(languages).length > 0" label="Idioma" label-class="font-weight-bold">
                    <b-input-group>
                      <b-input-group-prepend is-text><font-awesome-icon icon="language" /></b-input-group-prepend>
                      <b-form-select v-model="userAccount.langKey" :options="languageOptions"></b-form-select>
                    </b-input-group>
                  </b-form-group>
                </b-col>
              </b-row>

              <b-form-group label="Perfiles" label-class="font-weight-bold">
                <v-multiselect
                  :options="authorities"
                  :multiple="true"
                  :close-on-select="false"
                  :model-value="userAccount.authorities"
                  @update:model-value="userAccount.authorities = $event"
                  placeholder="Seleccione uno o más perfiles"
                  select-label="Presione enter para seleccionar"
                  deselect-label="Presione enter para remover"
                  selected-label="Seleccionado"
                >
                  <template #noResult>No se encontraron resultados</template>
                  <template #noOptions>La lista está vacía</template>
                </v-multiselect>
              </b-form-group>

              <div class="d-flex justify-content-end mt-5 border-top pt-4">
                <b-button variant="secondary" class="mr-3" @click="previousState()">
                  <font-awesome-icon icon="ban" />
                  <span class="ml-1" v-text="t$('entity.action.cancel')"></span>
                </b-button>
                <b-button variant="primary" type="submit" :disabled="v$.userAccount.$invalid || isSaving" style="min-width: 120px">
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

<script lang="ts" src="./user-management-edit.component.ts"></script>
