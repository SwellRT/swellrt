
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

      var c = cm.getCommunity();

      it("Set/Get Community Name", function(){
        c.setName("A community name");
        expect(c.getName()).toBe("A community name");
      });
    });


    describe("Community projects", function(){

      var community = cm.getCommunity();

      it("No projects for a new community", function(){
        var projects = community.getProjects();
        expect(projects).not.toBeUndefined();
        expect(projects.length).toBe(0);
      });



      it("Create project", function(){
        var project = community.addProject();
        expect(project).not.toBeUndefined();
        expect(project.getId()).toContain("prj+");

        var projects = community.getProjects();
        expect(projects).not.toBeUndefined();
        expect(projects.length).toBe(1);
      });


      it("Remove project", function(){
          var projects = community.getProjects();
          var project = projects[0];

          community.removeProject(project.getId());
          projects = community.getProjects();
          expect(projects.length).toBe(0);
        });
    });


    describe("Project simple properties", function(){

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

