import { registerPlugin } from '@capacitor/core';

import type { custominappbrowserPlugin } from './definitions';

const custominappbrowser = registerPlugin<custominappbrowserPlugin>('custominappbrowser', {
  web: () => import('./web').then((m) => new m.custominappbrowserWeb()),
});

export * from './definitions';
export { custominappbrowser };
