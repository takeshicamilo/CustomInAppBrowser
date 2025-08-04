export interface custominappbrowserPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  openUrl(options: { url: string }): Promise<{ success: boolean }>;
}
