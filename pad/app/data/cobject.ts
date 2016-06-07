export interface CObject {

  id(): string;
  domain(): string;

  createText(initValue: string);
  createString(initValue: string);
  addParticipant(participant: string);

  root: any;

}
