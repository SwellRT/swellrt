
var P2PvalueTest = {

  // Params - cm : CommunityModelJS

  setTestSuite_CommunityModel : function(cm) {



    describe("Community", function(){

      it("The community model exists", function(){
          expect(cm).not.toBeUndefined();
        });


      it("A community exists", function(){
        expect(cm.getCommunity()).not.toBeUndefined();
      });

    });


    describe("Participants", function(){



    });


    describe("Community simple properties", function(){



      it("Set/Get Community Name", function(){
        cm.getCommunity().setName("A community name");
        expect(cm.getCommunity().getName()).toBe("A community name");
      });
    });




    describe("Community projects", function(){



      it("No projects for a new community", function(){
        var projects = cm.getCommunity().getProjects();
        expect(projects).not.toBeUndefined();
        expect(projects.length).toBe(0);
      });

      it("Create project", function(){
        var project = cm.getCommunity().addProject();
        expect(project).not.toBeUndefined();
        expect(project.getId()).toContain("prj+");

        var projects = cm.getCommunity().getProjects();
        expect(projects).not.toBeUndefined();
        expect(projects.length).toBe(1);
      });


      it("Remove project", function(){
          var projects = cm.getCommunity().getProjects();
          var project = projects[0];

          cm.getCommunity().removeProject(project.getId());
          projects = cm.getCommunity().getProjects();
          expect(projects.length).toBe(0);
        });

      xit("Iterate projects", function(){
        // TBC
      });

    });



    describe("Project simple properties", function(){


      it("Set/Get Project Name", function(){
       var project = cm.getCommunity().addProject();
        project.setName("A project name");
        expect(project.getName()).toBe("A project name");
      });

      it("Set/Get Project Status", function(){
        var project = cm.getCommunity().getProjects()[0];
          project.setStatus("A project status");
          expect(project.getStatus()).toBe("A project status");
        });

      it("Set/Get Project Description", function(){
        var project = cm.getCommunity().getProjects()[0];
          project.setDescription("A project description");
          expect(project.getDescription()).toBe("A project description");
        });
    });



    xdescribe("Project Tasks", function(){

      it("No tasks for a new project", function(){
       var project = cm.getCommunity().getProjects()[0];
        expect(project.getNumTasks()).toBe(0);
        expect(project.getTasks().length).toBe(0);
      });

      var task;

      it("Create task", function() {
       var project = cm.getCommunity().getProjects()[0]
        task = project.addTask("A task name");
        expect(task).not.toBeUndefined();
        expect(task.getName()).toBe("A task name");
        expect(project.getNumTasks()).toBe(1);
        expect(project.getTask(0)).toBe(task);
      });

      it("Delete task", function() {
       var project = cm.getCommunity().getProjects()[0]
       var task = project.getTask(0);
       project.removeTask(task);
       expect(project.getNumTasks()).toBe(0);
       expect(project.getTask(0)).toBeUndefined();
      });

      xit("Iterate tasks", function() {
        // TBC
      });

    });

  },


  run : function() {

    var jasmineEnv = jasmine.getEnv();

    var waveletId = WaveJS.createCommunityModel(

        function(communityModel) {

          console.log("Launching test suite for New Community Model...");

          // Define tests
          P2PvalueTest.setTestSuite_CommunityModel(communityModel);

          // Execute tests
          jasmineEnv.execute();

         },

        function(error) {
            console.log("Error creating a New Community Model: "+ error)
          });

    if (waveletId != '') {
      console.log("Testing new wavelet with id: "+ waveletId);
    }

  }


};

