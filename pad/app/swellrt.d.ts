interface SwellRT {
  on(event: string, callback: Function);
  domain(): string;

  startSession(url: string, name: string, password: string, onSuccess: Function, onError: Function);
  stopSession();

  resumeSession(onSuccess: Function, onError: Function);

  openModel(id: string, onLoad: Function);
  closeModel(id: string);

  editor(parentElementId: string, widgets: any, annotations: any);

  createUser(parameters: {id: string, password: string, email?: string, locale?: string, avatarData?: string}, onComplete: Function): Promise<any>;


  events: any;
}

declare let SwellRT: SwellRT;
