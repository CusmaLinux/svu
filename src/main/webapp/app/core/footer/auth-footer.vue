<template>
  <footer class="bg-dark text-white mt-5 p-4 br-lg">
    <b-container>
      <b-row class="justify-content-between align-items-center">
        <b-col md="auto" class="text-center text-md-left mb-3 mb-md-0">
          <div class="d-flex align-items-center justify-content-center justify-content-md-start">
            <b-link to="/" aria-label="Página de inicio">
              <span class="logo-img"></span>
            </b-link>
            <span class="ml-3"
              >© {{ currentYear }}
              <b-link href="https://www.itp.edu.co/" target="_blank" class="text-white"> Instituto Tecnológico del Putumayo. </b-link></span
            >
          </div>
        </b-col>

        <b-col md="auto" class="text-center text-md-right">
          <ul class="list-inline mb-0">
            <li class="list-inline-item mx-2">•</li>
            <li class="list-inline-item">
              <b-link to="/account/settings" class="text-white">Mi Perfil</b-link>
            </li>
            <li class="list-inline-item mx-2">•</li>
            <li class="list-inline-item">
              <b-link href="mailto:atencionalusuario@itp.edu.co" class="text-white"> Soporte Técnico </b-link>
            </li>
          </ul>
        </b-col>
      </b-row>
    </b-container>
  </footer>
</template>

<script lang="ts" src="./auth-footer.component.ts"></script>

<style scoped>
a {
  text-decoration: none;
  transition: opacity 0.2s ease-in-out;
}

a:hover {
  text-decoration: none;
  opacity: 0.8;
}

.logo-img {
  display: inline-block;
  vertical-align: middle;
  height: 45px;
  width: 55px;
  background: url('/content/images/logo-svu.svg') no-repeat center center;
  background-size: contain;
  filter: drop-shadow(0 0 0.05rem white);
}

.list-inline-item {
  font-size: 0.9rem;
}
</style>
