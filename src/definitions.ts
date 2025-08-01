export interface custominappbrowserPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
