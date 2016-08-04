export interface CObject {

  root: any;

  id(): string;
  domain(): string;

  createText(initValue: string);
  createString(initValue: string);
  addParticipant(participant: string);

}
