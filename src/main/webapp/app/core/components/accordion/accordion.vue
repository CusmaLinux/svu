<template>
  <div class="svu-accordion-container">
    <div class="accordion" role="tablist">
      <b-card no-body v-for="(item, index) in items" :key="index" class="svu-accordion-card">
        <b-card-header header-tag="header" role="tab" class="p-0">
          <div v-b-toggle="`accordion-${index}`" class="svu-accordion-header">
            <span>{{ item.question }}</span>
          </div>
        </b-card-header>

        <b-collapse :id="`accordion-${index}`" accordion="svu-accordion" role="tabpanel">
          <b-card-body>
            <b-card-text>{{ item.answer }}</b-card-text>
          </b-card-body>
        </b-collapse>
      </b-card>
    </div>
  </div>
</template>

<script src="./accordion.component.ts" lang="ts"></script>

<style scoped>
.svu-accordion-container {
  background-color: #f8f9fa;
  padding: 40px 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.svu-accordion-card {
  margin-bottom: 20px;
  border: none !important;
  border-radius: 12px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  padding: 0.5rem;
}

.svu-accordion-header {
  padding: 20px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  color: #2c3e50;
  font-weight: 500;
  font-size: 1.05rem;
  background-color: #ffffff;
  position: relative;
  transition: background-color 0.2s ease;
}

.svu-accordion-header:focus {
  outline: none;
  box-shadow: none;
}

.svu-accordion-header:hover {
  background-color: #fdfdfd;
}

.svu-accordion-header::after {
  content: '';
  flex-shrink: 0;
  width: 1.25rem;
  height: 1.25rem;
  margin-left: 1rem;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' fill='%233b4d6b'%3e%3cpath fill-rule='evenodd' d='M1.646 4.646a.5.5 0 0 1 .708 0L8 10.293l5.646-5.647a.5.5 0 0 1 .708.708l-6 6a.5.5 0 0 1-.708 0l-6-6a.5.5 0 0 1 0-.708z'/%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-size: 1.25rem;
  transition: transform 0.3s ease-in-out;
}

/* Rotate the chevron icon when the accordion is opened */
/* The 'not-collapsed' class is automatically added by BootstrapVue to the trigger element */
.svu-accordion-header.not-collapsed::after {
  transform: rotate(-180deg);
}

/* Ensure the Bootstrap card-header has no default styles that interfere */
.svu-accordion-card .card-header {
  background-color: #ffffff;
  border-bottom: none;
}

/* Styling for the content area inside the accordion */
.svu-accordion-card .card-body {
  padding: 0px 20px 20px 20px;
  color: #5a6a85; /* A softer color for the answer text */
  line-height: 1.6;
}

.b-card-header {
  padding: 0;
}
</style>
