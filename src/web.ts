import { WebPlugin } from '@capacitor/core';

import type { custominappbrowserPlugin } from './definitions';

export class custominappbrowserWeb extends WebPlugin implements custominappbrowserPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
