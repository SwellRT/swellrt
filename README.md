Apache Wave
------------
The Apache Wave project is a stand alone wave server and rich web client
that serves as a Wave reference implementation.
Apache Wave site: http://incubator.apache.org/wave/.  
This project lets developers and enterprise users run wave servers and
host waves on their own hardware. And then share those waves with other
wave servers.  

Cryptographic Software Notice
-----------------------------
This distribution includes cryptographic software.  The country in
which you currently reside may have restrictions on the import,
possession, use, and/or re-export to another country, of
encryption software.  BEFORE using any encryption software, please
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to
see if this is permitted.  See <http://www.wassenaar.org/> for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this Apache Software Foundation
distribution makes it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception (see the BIS
Export Administration Regulations, Section 740.13) for both object
code and source code.

The following provides more details on the included cryptographic
software:

  Wave requires the BouncyCastle Java cryptography APIs:
    http://www.bouncycastle.org/java.html

Run Binary
----------
The nightly binaries can be downloaded from https://builds.apache.org/view/S-Z/view/Wave/job/wave-artifacts/lastSuccessfulBuild/artifact/.  
The latest "dev" releases can be downloaded from: https://dist.apache.org/repos/dist/dev/incubator/wave/.  
The latest officially released binaries can be downloaded from: https://dist.apache.org/repos/dist/release/incubator/wave/.  
Extract the archive and execute run-server.sh for Linux/Mac or run-server.bat for Windows.   
The web client will be accessible by default at http://localhost:9898/.

Build
------

Wave in a Box requires Java 7 & and uses Ant 1.9.3 (or higher) to build.

To run the tests (optional), run:   
    ant get-third-party test

To build the client and server run:  
    ant get-third-party compile-gwt dist-server  
It will be created in dist/wave-in-a-box-server-X.Y-incubating.jar.  

You need to configure your instance before you can use it. To create a default simple configuration run:  
    ant -f server-config.xml  

To override default values pass them to the ant script. 
For example, to override wave\_server\_domain run:  
ant -f server-config.xml -Dwave\_server\_domain=example.com  
Take a look at the server.config.example to learn about configuration and possible/default values.

The server can be started (on Linux/MacOS) by running  
    ./run-server.sh    
Or on Windows by running  
    run-server.bat  
Or, you can run the server from the compiled classes with ant:  
    ant run-server  
The web client will be accessible by default at http://localhost:9898/.


To learn more about Wave in a Box and Wave Federation Protocol:   
------
1. Subscribe to the wave-dev mailing list, find instructions at http://incubator.apache.org/wave/mailing-lists.html.  
2. Visit the Apache Wave wiki at https://cwiki.apache.org/confluence/display/WAVE/Home.
3. Look at the white papers folder - the information is a bit old but still usable.   
4. Watch the Wave Summit videos on YouTube, find the links at: https://cwiki.apache.org/confluence/display/WAVE/Wave+Summit+Talks

Protocol Buffers
--
Wave Protocol communicates using Protocol Buffers <http://code.google.com/p/protobuf/>.

Because of the difficulty of distributing binaries, we do not include the 
protocol compiler in this distribution. Therefore, to rebuild updated 
protocol buffer files, you will need to install the binary protoc 
in your environment by fetching the protobuf code from the website 
above.

Additionally, you will have to update the build-proto.properties file to
point to the unpacked source distribution of the protocol buffers release.

Then, after updating a proto file, run  
    ant -f build-proto.xml compile compile_json  

Note: this generates files into proto_src. If files here exist without
write permission, you will get permission denied errors from this step.

Note also that you don't need protoc unless you want to change the proto
files.


To enable SSL:
--
Create a Java keystore for your server (e.g. using http://portecle.sourceforge.net/).
You will need a key (e.g. called "server") whose subject Common Name (CN) is
the hostname of your server.

Set enable_ssl = true and set the ssl_keystore_path and ssl_keystore_password options.


To enable X.509 client authentication:

If your users have X.509 certificates which include their email address, you can have
them logged in automatically (with their wave ID being the same as their email address):
You can get X.509 certificates issued from any normal CA (e.g. StartSSL offer them for free).
You can get your CA's certficate from their website, though note they might provide more than 1 certificate which you need to chain before your client certificates are considered trusted.

1. Add the signing CA to your keystore file.
2. Set enable_clientauth = true
3. Set clientauth_cert_domain (to the part after the "@" in your email addresses).
4. (optional) Set disable_loginpage = true to prevent password-based logins.

Users will be automatically logged in when they access the site, with the
username taken from the email address in their certificate.

Setting up third party optional dependencies:   

To enable MongoDB:
--
In order to specify MongoDB in server.config as the storage option for storing deltas, accounts and attachments - you need to install according to instructions at: http://www.mongodb.org/downloads.  
Or on Ubuntu Linux you can use the following command:  
    sudo apt-get install mongodb-org

To enable Solr:
--
In order to specify Solr in server.config as the search type - you need to install Solr according to instructions at: http://www.apache.org/dyn/closer.cgi/lucene/solr/4.9.0.  
Or, you can use built in Ant script, i.e. run:  
    ant get-third-party-solr-dep  
This will download and unzip the Solr distribution into third_party/solr folder.  
You can then run the Solr server with:  
    run-solr.sh  
for Linux/Mac or:  
    run-solr.bat  
for Windows.  
