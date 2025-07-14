<template>
  <footer class="bg-dark text-white mt-5 p-4 br-lg">
    <b-container>
      <b-row>
        <b-col md="4">
          <div class="logo" b-link to="/">
            <h5 class="logo-title">Ventanilla Única</h5>
            <span class="logo-img"></span>
          </div>
          <p class="mt-3">
            Una solución para la gestión eficiente de PQRSD en la institución de educación superior <strong>UniPutumayo</strong>.
          </p>
        </b-col>
        <b-col md="4">
          <h5 class="text-uppercase footer-title">Enlaces Rápidos</h5>
          <ul class="list-unstyled">
            <li>
              <router-link to="/#section-welcome" class="text-white"> Inicio </router-link>
            </li>
            <li>
              <a class="text-white" href="https://www.itp.edu.co/" target="_blank"> Sobre Nosotros</a>
            </li>
            <li>
              <router-link to="/#section-faq" class="text-white"> Preguntas Frecuentes</router-link>
            </li>
          </ul>
        </b-col>
        <b-col md="4">
          <h5 class="text-uppercase footer-title">Contacto</h5>
          <ul class="list-unstyled">
            <li><font-awesome-icon icon="location-dot" class="mr-2" />Mocoa, Putumayo, Colombia</li>
            <li><font-awesome-icon icon="envelope" class="mr-2" />atencionalusuario@itp.edu.co</li>
            <li><font-awesome-icon icon="phone" class="mr-2" />+57 3138052807 <strong> - </strong> +57 3103310083</li>
          </ul>
        </b-col>
      </b-row>
      <div class="text-center py-3 mt-4 border-top-light">
        <p>
          © {{ currentYear }} <b-link href="https://www.itp.edu.co/" target="_blank" class="text-white"> UniPutumayo.</b-link> Todos los
          derechos reservados.
        </p>
      </div>
    </b-container>
  </footer>
</template>

<script lang="ts" src="./footer.component.ts"></script>

<style scoped>
.border-top-light {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

a {
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.footer-title {
  font-weight: 600;
  letter-spacing: 0.8px;
  padding-bottom: 1rem;
  margin-bottom: 1.5rem;
  position: relative;
}

/* Decorative underline */
.footer-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 40px;
  height: 2px;
  background-color: #00b8d4;
}

.logo-title {
  font-size: 1.5rem;
  font-weight: 700;
  text-transform: uppercase;
}

.logo .logo-img {
  height: 100px;
  display: inline-block;
  vertical-align: middle;
  width: 115px;
}

.logo-img {
  height: 100%;
  background: url('/content/images/logo-svu.svg') no-repeat center center;
  background-size: contain;
  width: 100%;
  filter: drop-shadow(0 0 0.05rem white);
  margin: 0 5px;
}
</style>
