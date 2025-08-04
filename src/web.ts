import { WebPlugin } from '@capacitor/core';

import type { custominappbrowserPlugin } from './definitions';

export class custominappbrowserWeb extends WebPlugin implements custominappbrowserPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async openUrl(options: { url: string }): Promise<{ success: boolean }> {
    console.log('OPEN URL (Web)', options);
    try {
      window.open(options.url, '_blank');
      return { success: true };
    } catch (error) {
      console.error('Failed to open URL:', error);
      return { success: false };
    }
  }
}
