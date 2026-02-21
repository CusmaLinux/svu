<script setup lang="ts">
import { computed } from 'vue';
import { usePermissions } from '@/shared/composables/use-permissions';
import type { Action, Subject } from '@/shared/composables/use-permissions';

interface Props {
  to: any;
  action: Action;
  subject: Subject;
  element?: string;
}

const props = withDefaults(defineProps<Props>(), {
  element: 'span',
});

const { can } = usePermissions();

const hasPermissions = computed(() => can(props.action, props.subject));
</script>

<template>
  <router-link v-if="hasPermissions" :to="to">
    <slot />
  </router-link>

  <component :is="element" v-else class="secure-text">
    <slot />
  </component>
</template>

<style scoped>
.secure-text {
  color: inherit;
  cursor: inherit;
}
</style>
