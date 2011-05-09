package xtremweb.role.cmdline;

/**
 * CommandLineTool.java
 *
 *
 * Created: Fri Apr 14 10:35:22 2006
 *
 * @author <a href="mailto:Gilles.Fedak@inriq.fr">Gilles Fedak</a>
 * @version 1.0
 */
import xtremweb.api.bitdew.*;
import xtremweb.api.activedata.*;
import xtremweb.api.transman.*;
import xtremweb.serv.dc.*;
import xtremweb.serv.ds.*;
import xtremweb.core.iface.*;
import xtremweb.core.log.*;
import xtremweb.core.com.com.*;
import xtremweb.core.com.idl.*;
import xtremweb.core.serv.*;
import xtremweb.role.ui.*;
import xtremweb.core.obj.dc.Data;
import xtremweb.core.obj.dr.Protocol;
import xtremweb.core.obj.dc.Locator;
import xtremweb.core.obj.dt.Transfer;
import xtremweb.core.obj.ds.Attribute;
import xtremweb.serv.ds.AttributeType;
import xtremweb.serv.dt.OOBException;
import xtremweb.serv.dt.OOBTransfer;
import xtremweb.serv.dt.OOBTransferFactory;
import xtremweb.core.http.*;
import java.io.*;
import jargs.gnu.CmdLineParser;
import java.util.Vector;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/*!
 * @defgroup cmdline Using BitDew
 *  @{

 @section Quickstart

 The simplest way is to use the file @c bitdew-stand-alone-x.y.z.jar  which contains all the files and libraries inclued in one signle jar file.

 You can launch the command-line tool simply with the following command, which will display the usage :

 @code
 java -jar bitdew-stand-alone-x.y.z.jar
 @endcode

 and to obtain the complete list of options :

 @code
 java -jar bitdew-stand-alone-x.y.z.jar --help
 @endcode

 The tool can either start services or act as a client. 

 To start all of the services supported by BitDew simply run the  following command :

 @code
 java -jar bitdew-stand-alone-x.y.z.jar serv dc dr dt ds
 @endcode

 This will start the following services dc : Data Catalog, dr : Data Repository, dt : Data Transfer and ds: Data Scheduling.


 @section Invoking the command line tool

 The format for running the BitDew command line program is:

 @code
 java -jar  bitdew-stand-alone-x.y.z.jar [Options] Commands [Command Options]
 @endcode

 If the command line seems too long to type for you, we recommand to set an alias in your @file{.bashrc} as this :

 @code
 alias bitdew="java -jar  /path_to/bitdew-stand-alone-x.y.z.jar "
 @endcode

 @subsection basic Basic data operation

 To create a new data in BitDew from an existing file (for instance @c /tmp/foo.zip), simply run the command put with the name of the file, which will produce the following output :

 @code
 % java -jar  bitdew-stand-alone-x.y.z.jar put /tmp/foo.zip
 @endcode

 To schedule, you have to create an attribute and 

 @subsection help Obtaining help

 BitDew supports the following options, shown by
 the output of @c java -jar  bitdew-stand-alone-x.y.z.jar @c --help:


 @code
 BitDew command line client

 Usage : java -jar bitdew-stand-alone.jar [Options] Commands [Command Options]

 Options:
 -h, --help                    display this helps
 -v, --verbose                 display debugging information
 -d, --dir                     working directory
 --host                    service hostname
 --port                    service port

 Services:
 serv [dc|dr|dt|ds]        start the list of services separated by a space

 Attributes:
 attr attr_definition      create attribute where attr_definition has the syntax att_Name 
 = {field1=value1, field2=value2}.
 Field can have the following values :
 replicat=int          number of data replicat in the system. The special value -1    
 means that the data will be replicated to each node
 affinity=dataId       affinity to data Identifier. Schedule the data on node where   
 dataId is present.
 lftabs=int            absolute life time. The value is the life duration in minutes.
 lftabs=dataId         relative lifetime. The data will be obsolete when dataId is    
 deleted.
 oob=protocol          out-of-band file transfer protocol. Protocol can be one of the 
 following [dummy|ftp|bittorrent]
 ft=[true|false]       fault tolerance. If true data will be rescheduled if one host  
 holding the data is considered as dead.
 distrib=int           maximum number of data of this attribute, a host can hold. The 
 special value -1  means that this number is infinite

 Data:
 data file_name            create a new data from the file file_name

 Scheduling:
 sched attr_uid data_uid [data_uids ..... ]
 associate and attribute given by its uid to one one or several 
 data

 File:
 put file_name [dataId]    copy a file in the data space. If dataId is not specified, a new
 data will be created from the file.
 get dataId [file_name]    get the file from dataId.
 @endcode

 @subsection create Creating Data

 @subsection file Moving Files to and from the Data Space

 * @}
 */

public class CommandLineTool {

	private BitDew bitdew;
	private ActiveData activeData;
	private TransferManager transferManager = null;

	private enum HelpFormat {
		SHORT, LONG
	};

	private String host;
	private String attruid;
	private String fileName;
	private String dirName;
	private int port;
	private boolean verbose;
	private boolean server = false;
	private static Logger log = LoggerFactory.getLogger("CommandLineTool");

	public CommandLineTool(String[] args) {

		// force the log4J configuration to log level info without formatting
		if (log instanceof Log4JLogger) {
			try {
				Log4JLogger.setProperties("conf/log4jcmdlinetool.properties");
			} catch (LoggerException le) {
				log.debug(le.toString());
			}
		}

		String[] otherArgs = parse(args);

		// switch to verbose mode
		if (verbose)
			log.setLevel("debug");

		// if there's no other argument display helps
		if (otherArgs.length == 0)
			usage(HelpFormat.SHORT);

		// start services
		if (otherArgs[0].equals("serv")) {
			boolean skipserv = false;
			Vector services = new Vector();
			for (String s : otherArgs) {
				// TODO the rest of the command line will be tried to be loaded
				// as service
				// that would be better to try scan for the available services
				if (skipserv)
					services.add(s);
				else
					skipserv = true;
			}
			ServiceLoader sl = new ServiceLoader("RMI", port, services);
			UIFactory.createUIFactory();
			server = true;
			return;
		} else {
			try {
				Vector comms = ComWorld.getMultipleComms(host, "rmi", port,
						"dc", "dr", "dt", "ds");
				activeData = new ActiveData(comms);
				bitdew = new BitDew(comms);
				transferManager = new TransferManager(comms);
			} catch (ModuleLoaderException e) {
				log.warn("Cannot find service " + e);
				log.warn("Make sure that your classpath is correctly set");
				System.exit(0);
			}
		}
		// add a protocol via commandLine
		if (otherArgs[0].equals("add")) {
			add(otherArgs);
		}
		// create attr
		if (otherArgs[0].equals("attr")) {
			if (otherArgs.length == 1)
				usage(HelpFormat.LONG);
			Attribute attr = null;
			try {
				attr = AttributeUtil.parseAttribute(otherArgs[1]);
			} catch (ActiveDataException ade) {
				log.warn(" Cannot parse attribute definition : " + ade);
			} catch (JsonSyntaxException sex) {
				log.fatal("Syntax exception " + sex.getMessage());
			}
			try {
				Attribute _attr = activeData.registerAttribute(attr);
				log.info("attribute registred : "
						+ AttributeUtil.toString(_attr));
			} catch (ActiveDataException ade) {
				log.warn(" Cannot registrer attribute : " + ade);
				System.exit(0);
			}
		}
		try {
			// create data
			if (otherArgs[0].equals("data")) {
				JsonObject jsono = new JsonParser().parse(otherArgs[1])
						.getAsJsonObject();
				File f;
				String s, file = null, str = null;
				Data data = null;
				if (jsono.get("file") == null && jsono.get("string") == null) {
					log.fatal("Syntax error, see usage ");
					System.exit(0);
				}
				if (jsono.get("file") != null && jsono.get("string") != null) {
					log.fatal("Syntax error, see usage ");
					System.exit(0);
				}
				if (jsono.get("file") != null)
					file = jsono.get("file").getAsString();
				if (jsono.get("string") != null)
					str = jsono.get("string").getAsString();
				if (file != null) {
					f = new File(file);
					if (!f.exists()) {
						log.warn(" File does not exist : " + otherArgs[1]);
						System.exit(0);
					}
					data = bitdew.createData(f);
					log.info("Data registred : " + DataUtil.toString(data));
				}
				if (str != null) {
					data = bitdew.createData(str);
					log.info("Data registred : " + DataUtil.toString(data));
				}
			}
		} catch (BitDewException ade) {
			log.warn(" Cannot registrer data : " + ade);
			System.exit(0);
		} catch (java.lang.IllegalStateException exc) {
			log
					.warn("Not a json object, probably you have spaces in your JSON object, if is the case use quotation marks");
		}
		// create

		// schedule data and attribute
		if (otherArgs[0].equals("sched")) {

			String jsonize = CommandLineToolHelper.jsonize(otherArgs[1]);
			JsonObject jsono = new JsonParser().parse(jsonize)
					.getAsJsonObject();
			if (jsono.get("attr_uid") == null) {
				log.fatal("Attribute id is mandatory");
				System.exit(0);
			}
			String attr_uid = jsono.get("attr_uid").getAsString();
			if (jsono.get("data_uids") == null) {
				log.fatal("Datas must be associated with attribute");
				System.exit(0);
			}
			JsonArray array = jsono.get("data_uids").getAsJsonArray();
			if (array.size() == 0) {
				log.fatal("Data array cannot be empty");
				System.exit(0);
			}
			// verify that this attribute exists
			Attribute attr = null;
			try {
				attr = activeData.getAttributeByUid(attr_uid);
			} catch (ActiveDataException ade) {
				log.info("Attribute with uid " + attr_uid
						+ " doesn't exist in the system : " + ade);
				System.exit(2);
			}

			// build the list of data to schedule and check them
			ArrayList<Data> toSchedule = new ArrayList<Data>();
			for (int i = 0; i < array.size(); i++) {
				try {
					Data d = bitdew.searchDataByUid(array.get(i).getAsString());
					if (d != null) {
						toSchedule.add(d);
					} else
						log.info("Error : Data with uid " + otherArgs[i]
								+ " doesn't exist in the system ");
				} catch (BitDewException bde) {
					log.info("Data with uid " + otherArgs[i]
							+ " doesn't exist in the system : " + bde);
				}
			}

			// exit if there is nothing to do
			if (toSchedule.isEmpty())
				System.exit(2);

			// schedule the data list
			String msg = "Scheduling Data : ";
			for (Data data : toSchedule) {
				try {
					activeData.schedule(data, attr);
					if (verbose)
						msg += "\n" + DataUtil.toString(data);
					else
						msg += "[" + data.getname() + "|" + data.getuid()
								+ "] ";
				} catch (ActiveDataException ade) {
					log.info("Unable to schedule data " + "[" + data.getname()
							+ "|" + data.getuid() + "] " + "with attribute "
							+ AttributeUtil.toString(attr) + " : " + ade);
				}
			}
			String tmp = AttributeUtil.toString(attr);
			log.info(msg.substring(0, msg.length() - 1) + (verbose ? "\n" : "")
					+ " with Attribute " + tmp.substring(5, tmp.length()));
		}// schedulde

		// put file [dataId]
		if (otherArgs[0].equals("put")) {
			OOBTransfer noobt = null;
			if ((otherArgs.length != 3) && (otherArgs.length != 4))
				usage(HelpFormat.LONG);

			File file = new File(otherArgs[1]);
			if (!file.exists()) {
				log.warn(" File does not exist : " + otherArgs[1]);
				System.exit(0);
			}
			String myprot = null;
			Data data = null;
			try {
				// no dataId
				if (otherArgs.length == 3) {
					myprot = otherArgs[2];
					data = bitdew.createData(file);
					log.info("Data registred : " + DataUtil.toString(data));
				} else {
					data = bitdew.searchDataByUid(otherArgs[2]);
					if (data == null) {
						log.info("cannot find data whose uid is : "
								+ otherArgs[2]);
						System.exit(0);
					}
					bitdew.updateData(data, file);
				}
			} catch (BitDewException ade) {
				log.warn(" Cannot registrer data : " + ade);
				System.exit(0);
			}
			try {
				OOBTransfer oobTransfer = bitdew.put(file, data, myprot);
				Vector comms = ComWorld.getMultipleComms(host, "rmi", port,
						"dr", "dc", "dt");
				TransferManager transman = TransferManagerFactory
						.getTransferManager((InterfaceRMIdr) comms.get(0),
								(InterfaceRMIdt) comms.get(2));
				Protocol protoc = oobTransfer.getRemoteProtocol();
				System.out.println(" login i s " + protoc.getlogin());
				String passwd = protoc.getpassword();
				String passphrase = protoc.getpassphrase();
				if (passwd != null && passwd.equals("yes"))
					oobTransfer = promptPassword(protoc, oobTransfer);
				if (passphrase != null && passphrase.equals("yes"))
					oobTransfer = promptPassphrase(protoc, oobTransfer);
				transman.registerTransfer(oobTransfer);
				log.debug("Succesfully created OOB transfer " + oobTransfer);
				transman.waitFor(data);
				transman.stop();
				log.info("Transfer finished");
			} catch (TransferManagerException tme) {
				log.warn(" Transfer data : " + tme);
				System.exit(0);
			} catch (BitDewException bde) {
				log.warn(" Cannot transfer data : " + bde);
				System.exit(0);
			} catch (ModuleLoaderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OOBException e) {
				log.fatal(" OOBException ");
				e.printStackTrace();
			}
		}// put

		// get dataId file
		if (otherArgs[0].equals("get")) {
			if ((otherArgs.length != 3) && (otherArgs.length != 4))
				usage(HelpFormat.LONG);

			String dataUid = otherArgs[1];

			// check the dataId
			Data data = null;
			try {
				data = bitdew.searchDataByUid(dataUid);
				if (data == null) {
					log.info("cannot find data whose uid is : " + dataUid);
					System.exit(0);
				}
				log.info("Data registred : " + DataUtil.toString(data));
			} catch (BitDewException ade) {
				log.warn(" Cannot search data : " + ade);
				System.exit(0);
			}
			String prot = otherArgs[2];
			// set the file name
			String fileName = null;
			if (otherArgs.length == 4) {
				fileName = otherArgs[3];
			} else {
				fileName = data.getname();
			}

			// check the file
			File file = new File(fileName);
			if (file.exists()) {
				log.warn("warning, you will overwrite " + fileName
						+ ". Do you really want to continue (y/N) ?");
				try {
					BufferedReader stdIn = null;
					stdIn = new BufferedReader(new InputStreamReader(System.in));
					int i = stdIn.read();
					if ((i != 121) && (i != 89))
						System.exit(0);
				} catch (IOException ioe) {
					log.warn("program interrupted" + ioe);
					System.exit(0);
				}
			}
			// get the data
			try {
				transferManager.start(true);
				data.setoob(prot);
				OOBTransfer oobt = bitdew.get(data, file);
				Protocol protoc = oobt.getRemoteProtocol();
				String passwd = protoc.getpassword();
				String passphrase = protoc.getpassphrase();
				if (passwd != null && passwd.equals("yes"))
					oobt = promptPassword(protoc, oobt);
				if (passphrase != null && passphrase.equals("yes"))
					oobt = promptPassphrase(protoc, oobt);
				transferManager.registerTransfer(oobt);
				log.debug("Succesfully created OOB transfer " + oobt);
				transferManager.waitFor(data);
				transferManager.stop();
				log.info("Transfer complete");
			} catch (TransferManagerException ade) {
				log.warn(" Transfer data : " + ade);
				System.exit(0);
			} catch (BitDewException bde) {
				log.warn(" Transfer data : " + bde);
				System.exit(0);
			} catch (OOBException e) {
				e.printStackTrace();
			}
		}// get
	} // CommandLineTool constructor

	public OOBTransfer promptPassphrase(Protocol protoc, OOBTransfer oobTransfer)
			throws OOBException {
		char[] passph;
		// TODO ojo !!! console es java 1.6
		Console c = System.console();
		passph = c.readPassword("[%s]", "Please insert passphrase for key : "
				+ protoc.getprivatekeypath());
		protoc.setpassphrase(new String(passph));
		java.util.Arrays.fill(passph, ' ');

		Data datan = oobTransfer.getData();
		Transfer t = oobTransfer.getTransfer();
		Locator remote_locator = oobTransfer.getRemoteLocator();
		Locator local_locator = oobTransfer.getLocalLocator();
		Protocol local_proto = oobTransfer.getLocalProtocol();
		return OOBTransferFactory.createOOBTransfer(datan, t, remote_locator,
				local_locator, protoc, local_proto);
	}

	public OOBTransfer promptPassword(Protocol protoc, OOBTransfer oobTransfer)
			throws OOBException {
		char[] password;
		// TODO ojo !!! console es java 1.6
		Console c = System.console();
		password = c.readPassword("[%s]",
				"Please insert password for server : " + protoc.getserver());
		String s = new String(password);
		protoc.setpassword(s);
		java.util.Arrays.fill(password, ' ');
		Data datan = oobTransfer.getData();
		Transfer t = oobTransfer.getTransfer();
		Locator remote_locator = oobTransfer.getRemoteLocator();
		Locator local_locator = oobTransfer.getLocalLocator();
		Protocol local_proto = oobTransfer.getLocalProtocol();
		return OOBTransferFactory.createOOBTransfer(datan, t, remote_locator,
				local_locator, protoc, local_proto);
	}

	public boolean isServer() {
		return server;
	}

	private String[] parse(String[] args) {

		// if there's no argument display helps
		if (args.length == 0)
			usage(HelpFormat.SHORT);

		CmdLineParser parser = new CmdLineParser();

		CmdLineParser.Option helpOption = parser.addBooleanOption('h', "help");
		CmdLineParser.Option verboseOption = parser.addBooleanOption('v',
				"verbose");
		CmdLineParser.Option dirOption = parser.addStringOption('d', "dir");
		CmdLineParser.Option hostOption = parser.addStringOption("host");
		CmdLineParser.Option fileOption = parser.addStringOption("file");
		CmdLineParser.Option attrOption = parser.addStringOption("attr");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			log.debug(e.getMessage());
			usage(HelpFormat.SHORT);
		}

		host = (String) parser.getOptionValue(hostOption, "localhost");
		attruid = (String) parser.getOptionValue(attrOption);
		fileName = (String) parser.getOptionValue(fileOption);
		dirName = (String) parser.getOptionValue(dirOption, ".");
		boolean help = ((Boolean) parser.getOptionValue(helpOption,
				Boolean.FALSE)).booleanValue();
		verbose = ((Boolean) parser
				.getOptionValue(verboseOption, Boolean.FALSE)).booleanValue();

		// if the help option is set display long help
		if (help)
			usage(HelpFormat.LONG);

		return parser.getRemainingArgs();
	}

	/**
	 * Add a json formatted repository
	 * 
	 * @param otherArgs
	 *            the original arguments , the json argument is in otherArgs[1]
	 */
	public void add(String[] otherArgs) {
		String object = otherArgs[1];

		String json = CommandLineToolHelper.jsonize(object);

		JsonObject repo = new JsonParser().parse(json).getAsJsonObject();
		// JsonObject repo = objectj.getAsJsonObject("repository");

		if (repo != null) {
			CommandLineToolHelper.notNull("name", repo.get("name"));
			CommandLineToolHelper.notNull("path", repo.get("path"));
			CommandLineToolHelper.notNull("server", repo.get("server"));
			CommandLineToolHelper.notNull("port", repo.get("port"));

			String name = (String) repo.get("name").getAsString();
			String path = (String) repo.get("path").getAsString();
			String host = (String) repo.get("server").getAsString();
			Long lon = (Long) repo.get("port").getAsLong();
			int port = lon.intValue();

			String login = CommandLineToolHelper
					.nullOrObject(repo.get("login"));
			String passwd = CommandLineToolHelper.nullOrObject(repo
					.get("passwd"));

			String knownhosts = CommandLineToolHelper.nullOrObject(repo
					.get("knownhosts"));
			String prkeypath = CommandLineToolHelper.nullOrObject(repo
					.get("prkeypath"));
			String pukeypath = CommandLineToolHelper.nullOrObject(repo
					.get("pukeypath"));
			System.out.println("passphrase is " + repo.get("passphrase"));
			String passphrase = CommandLineToolHelper.nullOrObject(repo
					.get("passphrase"));

			if (name.equals("http") || name.equals("ftp"))// TODO make it
				// extensible
				bitdew.registerNonSecuredProtocol(name, host, port, path,
						login, passwd);
			else
				bitdew.registerSecuredProtocol(login, name, host, port, path,
						knownhosts, prkeypath, pukeypath, passphrase);
		} else {
			log.info(" you need to describe a repository");
		}

	}

	public void usage(HelpFormat format) {
		Usage usage = new Usage();
		switch (format) {
		case LONG:
			usage.title();
			usage.ln();
			usage.section("BitDew command line client");
			usage.ln();
			usage
					.usage("java -jar bitdew-stand-alone.jar [Options] Commands [Command Options]");
			usage.ln();
			usage.section("Options:");
			usage.option("-h", "--help", "display this helps");
			usage.option("-v", "--verbose", "display debugging information");
			usage.option("-d", "--dir", "working directory");
			usage.option("--host", "service hostname");
			usage.option("--port", "service port");
			usage.ln();
			usage.section("Services:");
			usage.option("serv [dc|dr|dt|ds]",
					"start the list of services separated by a space");
			usage.ln();
			usage.section("Attributes:");
			usage
					.option(
							"attr attr_definition",
							"create attribute where attr_definition has the syntax att_Name = {field1=value1, field2=value2}.");
			usage.option("", "Field can have the following values :");
			usage
					.option(
							"    replicat=int",
							"number of data replicat in the system. The special value -1    means that the data will be replicated to each node");
			usage
					.option(
							"    affinity=dataId",
							"affinity to data Identifier. Schedule the data on node where   dataId is present.");
			usage
					.option("    lftabs=int",
							"absolute life time. The value is the life duration in minutes.");
			usage
					.option("    lftabs=dataId",
							"relative lifetime. The data will be obsolete when dataId is    deleted.");
			usage
					.option(
							"    oob=protocol",
							"out-of-band file transfer protocol. Protocol can be one of the following [dummy|ftp|bittorrent]");
			usage
					.option(
							"    ft=[true|false]",
							"fault tolerance. If true data will be rescheduled if one host  holding the data is considered as dead.");
			usage
					.option(
							"    distrib=int",
							"maximum number of data of this attribute, a host can hold. The special value -1  means that this number is infinite");
			usage.ln();
			usage.section("Data:");
			usage.option("data file_name",
					"create a new data from the file file_name");
			usage.ln();
			usage.section("Scheduling:");
			usage
					.option("sched attr_uid data_uid [data_uids ..... ]",
							"schedule one or a list of data with the specified attribute");
			// usage.option("unsched data_uid [data_uids ..... ]","unschedule one or a list of data");
			usage.ln();
			usage.section("File:");
			usage
					.option(
							"put file_name [dataId]",
							"copy a file in the data space. If dataId is not specified, a new data will be created from the file.");
			usage.option("get dataId [file_name]", "get the file from dataId.");
			usage.ln();

			// usage.option("--file filename","file name to be created" );
			// usage.option("--attr attruid","attribute uid associated to the data"
			// );

			// usage.option("--file filename","file name to be created" );
			// usage.option("--attr attruid","attribute uid associated to the data"
			// );

			break;
		case SHORT:
			usage.usage("try java -jar bitdew-stand-alone-"
					+ Version.versionToString()
					+ ".jar [-h, --help] for more information");
			break;
		}
		System.exit(2);
	}

	public static void main(String[] args) {
		CommandLineTool cmd = new CommandLineTool(args);

		// FIXME We shouldn't have to explicitely exit
		if (!cmd.isServer())
			System.exit(0);

	}
} // CommandLineTool
