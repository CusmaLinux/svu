import { shallowMount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import Ribbon from './ribbon.vue';

type RibbonComponentType = InstanceType<typeof Ribbon>;

describe('Ribbon', () => {
  it('should not have ribbonEnabled when no data', () => {
    const wrapper = shallowMount(Ribbon, {
      global: {
        plugins: [createTestingPinia({ stubActions: false })],
      },
    });
    expect(wrapper.vm.ribbonEnabled).toBeFalsy();
  });

  it('should have ribbonEnabled set to value in store', async () => {
    const profile = 'dev';
    const wrapper = shallowMount(Ribbon, {
      global: {
        plugins: [
          createTestingPinia({
            stubActions: false,
            initialState: {
              main: {
                activeProfiles: ['foo', profile, 'bar'],
                ribbonOnProfiles: profile,
              },
            },
          }),
        ],
      },
    });
    expect(wrapper.vm.ribbonEnabled).toBeTruthy();
  });

  it('should not have ribbonEnabled when profile not activated', async () => {
    const profile = 'dev';
    const wrapper = shallowMount(Ribbon, {
      global: {
        plugins: [
          createTestingPinia({
            stubActions: false,
            initialState: {
              main: {
                activeProfiles: ['foo', 'bar'],
                ribbonOnProfiles: profile,
              },
            },
          }),
        ],
      },
    });
    expect(wrapper.vm.ribbonEnabled).toBeFalsy();
  });
});
