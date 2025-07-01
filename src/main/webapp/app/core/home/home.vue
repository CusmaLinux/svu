<template>
  <b-container class="py-5">
    <b-card no-body class="shadow-lg overflow-hidden">
      <b-row no-gutters>
        <b-col lg="6" class="position-relative">
          <b-img class="welcome-image" alt="Bienvenida PQRSD" src="/content/images/home-svu.webp" fluid-grow />
        </b-col>

        <b-col lg="6">
          <b-card-body class="p-5 d-flex flex-column justify-content-between h-100">
            <div>
              <h1 v-if="authenticated" class="font-weight-bolder mb-3">¡Bienvenido, {{ username }}!</h1>
              <p class="text-muted lead mb-4">Tu central de operaciones para la gestión eficiente de PQRSD.</p>

              <b-alert show variant="info" class="mb-4">
                <b-row>
                  <b-col cols="1">
                    <font-awesome-icon size="lg" icon="circle-info" />
                  </b-col>
                  <b-col>
                    <p class="font-weight-bold mb-1">Rol: {{ displayMainRole }} de PQRSD</p>
                    <p class="mb-0 small">Supervisa y gestiona las PQRSD para garantizar una respuesta oportuna y eficaz.</p>
                  </b-col>
                </b-row>
              </b-alert>

              <b-row class="mb-5">
                <b-col lg="12" xl="6" class="mb-3 mb-lg-0">
                  <b-button href="#" variant="primary" size="lg" class="w-100">
                    <font-awesome-icon icon="tachometer-alt" class="mr-2" />
                    Ver Panel
                  </b-button>
                </b-col>
                <b-col lg="12" xl="6">
                  <b-button href="#" variant="secondary" size="lg" class="w-100">
                    <font-awesome-icon icon="gear" class="mr-2" />
                    Configurar
                  </b-button>
                </b-col>
              </b-row>
            </div>

            <div>
              <h2 class="h4 font-weight-bold border-top pt-4 mb-4">Tareas Rápidas</h2>
              <b-list-group>
                <b-list-group-item to="/pqrs" class="font-weight-normal d-flex align-items-center">
                  <font-awesome-icon icon="tasks" class="mr-3 text-primary" size="lg" />
                  <span class="font-weight-medium">Revisar PQRSD Pendientes</span>
                </b-list-group-item>
              </b-list-group>
            </div>
          </b-card-body>
        </b-col>
      </b-row>
    </b-card>
  </b-container>
</template>

<script lang="ts" src="./home.component.ts"></script>
<style scoped>
.welcome-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
