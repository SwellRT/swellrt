var app = {

    // Swell stuff

    service: null, // swell service api
    object: null, // current active swell object
    messages: null, // message list from swell object

    // App properties
    settings: { chatid: "chat" },

    init: function(service, settings) {

        app.service = service;

        app.login()
        .then( r => {
            app.load();
        })
        .catch( app.handleError );
    },

    /**
     * Login in Swell server
     */
    login: function() {

        return app.service.login({
            id : swell.Constants.ANONYMOUS_USER_ID,
            password : ""
          });

    },

    /**
     * Load chat object from Swell server
     */
    load: function() {
        
        return app.service.open({
            id : app.settings.chatid
          })
          .then( object => {
            
            app.object = object;
            app.setupData();
            app.renderAll();
            app.setupInput();
         
          });
          
    },


    /** Setup chat object if necessary */
    setupData: function() {

        if (!app.object.node("messages")) {
            app.object.put("messages", swell.List.create());
            app.object.setPublic(true);
        }

        app.messages = app.object.node("messages");
        app.messages.listen(app.onMessage);

    },


    /** 
     * Enable sending messages from UI 
     */
    setupInput: function() {

        $('#send').on('click', function(e) {
            app.sendMessage();
        });

           
        $('#message').on('keypress', function(e) {
            
            if (e.keyCode == 13) {
                e.preventDefault();
                app.sendMessage();
            }
            
        });  
    },

    sendMessage: function() {

        var content = $('#message').val();
        
        if (content) {

            var message = {
                author : "unknown",
                time : new Date().getTime(),
                content : content
            };

            app.messages.add(message);

            $('#message').val('');
        }
    },

    /**
     * On chat new messages
     */
    onMessage: function(event) {
        
        if (event.isAddEvent()) {
            var message = event.value;
            var element = app.renderMessage(message);
            app.focusMessage(element);
        } 
        console.log(event.toString());
    },

    focusMessage: function(element) {
        $('.container-messages').animate({ scrollTop: element.prop('offsetTop')*2 }, 100);
    },

    renderAll: function() {
        var element;
        for (i=0; i < app.messages.size(); i++) {
            element = app.renderMessage(app.messages.pick(i).value);
        }
        if (element)
            app.focusMessage(element);
       
    },

    renderMessage: function(message) {

       var element = $(' <div class="panel panel-default"></div> ').append(
            $(' <div class="panel panel-default"></div> ').append(
                $(' <div class="panel-heading"> (' + message.author + ') ' + new Date(message.time) + '</div> '),
                $(' <div class="panel-body"> ' + message.content + '</div>') 
            )
        );

        element.appendTo('#messages');
        return element;
    },

    /**
     * Handle error
     */
    handleError: function(e) {
        alert(e);
    }

};