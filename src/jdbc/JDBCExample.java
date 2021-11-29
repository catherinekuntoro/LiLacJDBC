package jdbc; //this .java file is under a package named "jdbc"


//STEP 1. Import required packages
import java.sql.*;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class JDBCExample {
	// JDBC driver name and database URL

	//LiLac is the name of our database
	static final String DB_URL = "jdbc:mysql://localhost/LiLAC?serverTimezone=UTC";

	static final String USER = "root";
	static final String PASS = "ckckck12"; //your computer's password
	

	public static void main(String[] args) {
		Connection conn = null;

		try{
			//Open a connection
			conn = DriverManager.getConnection(DB_URL, USER, PASS);

			System.out.println("Welcome to LiLac Flower Shop! ");
			mainPromptHelper();
			Scanner scanner = new Scanner(System.in); 

			while(scanner.hasNextLine()) {
				
				String userInput = scanner.nextLine().toUpperCase();
				if(userInput.equals("A")) {
					viewBouquetInformation(conn);
				} else if (userInput.equals("B")) {
					viewDiscountEligibility(conn);
				} else if(userInput.equals("C")) {
					viewOrderHistory(conn);
				}else if (userInput.equals("D")) {
					orderBouquet(conn);	
				} else if (userInput.equals("E")) {
					leftJoinCommand(conn);	
				} else if (userInput.equals("F")) {
					viewCustomersSpentOver25(conn);	
				} else if (userInput.equals("G")) {
					viewAllItems(conn);
				}
				else if (userInput.equals("TEST")) {
					//updateBouquetNumCount(conn, 4); //DUMMY VALUE cath testing
					orderBouquet(conn);
				} else { // invalid input. 
					System.out.println("Invalid input.");
				}

				// Re-prompting to repeat procedure
				System.out.println();
				System.out.println("We could help you with these following activities: ");
				mainPromptHelper();
			}
			System.out.println("Goodbye!");
			scanner.close();
		}catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();

		} finally{
			//finally block used to close resources
			try{
				if(conn!=null)
					conn.close();
			}catch(SQLException se){
				se.printStackTrace();
			}

		}
	}//end main

	private static void mainPromptHelper() {
		System.out.println("Enter \"A\" to view bouquet information");
		System.out.println("Enter \"B\" to view your discount eligibility");
		System.out.println("Enter \"C\" to view your order history");
		System.out.println("Enter \"D\" to order a bouquet");
		System.out.println("Enter \"E\" to view users who hasn't bought any bouquets");
		System.out.println("Enter \"F\" to view customers who spent $25 and over");
		System.out.println("Enter \"G\" to view all items LiLAC might offer");
	}
	
	// called from main
	private static void leftJoinCommand(Connection conn) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			String sql = "select * FROM Customer Left Outer Join Sale on Customer.cID = Sale.cID";
			ResultSet rs = stmt.executeQuery(sql);
			
			while(rs.next()) {
				if(rs.getString("packaging") == null) {
					System.out.println("This customer hasn't bought any bouquet: " + rs.getString("cName"));
				}
			}
			rs.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(stmt!=null)
					stmt.close();
			}catch(SQLException se2){}// nothing we can do

		}
		
		
	}
	
	// using group by and having to find customers who spent a cumulative total over $25. called from main()
	private static void viewCustomersSpentOver25(Connection conn) {
		Statement stmt = null;
		try {
			String SQL ="select Customer.cName, sum(Sale.pricePaid) as cumulative_pricePaid from Sale, Customer, Bouquet where Sale.cID = Customer.cID AND Sale.bID = Bouquet.bID group by cName having sum(Sale.pricePaid) >= 25";
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(SQL);  

			//Display result
			System.out.println("Here are all the customers who spent a cumulative total of $25 and over.");
			while(rs.next()){
				System.out.println("Customer name = "+rs.getString("cName")+", cumulative price paid = "+ "$" +rs.getInt("cumulative_pricePaid"));
			}
			System.out.println();

		} catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(stmt!=null)
					stmt.close();
			}catch(SQLException se2){
			}
		}// nothing we can do
	}
	
	
	// Called from main(). Luis
	private static void viewAllItems(Connection conn) {
		String output = "";
		
		String SQL = "SELECT fName FROM Flower UNION SELECT bName FROM Bouquet";
		Statement stmt = null;
		ResultSet rs = null;
		
		System.out.println("Here are all the items we use or sell here at LiLAC");
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			
			while(rs.next()) {
				output += rs.getString(1) + "\n";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		System.out.println(output);
	}
	
	// called from main(). Luis
	private static void orderBouquet(Connection conn) {
		
		Scanner scanner = new Scanner(System.in);
		
		//Get customer name and customer ID with scanner
		System.out.print("Please enter your name: ");
		String cName = scanner.nextLine();
		//If customer DOES NOT exist in Customer schema, insert it. else, nothing
		PreparedStatement pstmt = null;
		String SQL = "SELECT * FROM Customer WHERE cName = ?";
		boolean hasResults = true;
		Statement statement = null;
		try {
			pstmt = conn.prepareStatement(SQL);
			pstmt.setString(1, cName);
			ResultSet rs = pstmt.executeQuery();
			hasResults = rs.isBeforeFirst();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// Customer does not exist in customer schema
		if(!hasResults) {
			int maxCID = 0;
			try {
				statement = conn.createStatement();
				ResultSet rs = statement.executeQuery("Select max(cID) from Customer");
				rs.next();
				maxCID = rs.getInt(1);
				maxCID++;
			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			SQL = "INSERT INTO CUSTOMER VALUES (?, ?, ?, ?)";
			try {
				pstmt = conn.prepareStatement(SQL);
				pstmt.setInt(1, maxCID);
				pstmt.setString(2, cName);
				pstmt.setBoolean(3, false); // discountUser false by default
				
				//pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
				pstmt.setDate(4, new Date(System.currentTimeMillis()));
				pstmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		
		
		boolean invalidInput = true;
		while(invalidInput) {
			//Prompt for bouquet type: existing type bouquet or a new bouquet. 
			System.out.println("Would you like to order an existing bouquet or a new bouquet?\n"
					+ "Enter X for ordering an existing bouquet or enter Y to order a new bouquet");
			
			String input = scanner.nextLine();
			input = input.toLowerCase();
			
			if(input.equals("x")) { // ordering existing bouquet
				invalidInput = false;
				System.out.println("Please enter the name of the bouquet you wish to buy: ");
				input = scanner.nextLine();
				buyBouquet(conn, input);
				
			} else if(input.equals("y")) { // order a new bouquet
				invalidInput = false;
				createNewBouquetType(conn); // IF user wants a new bouquet
			}else {
				System.out.println("Invalid input. Trying again...");
			}
		}
		scanner.close();
	}

	// called from orderBouquet() or createNewBouquet() -> Luis
	private static void buyBouquet(Connection conn, String userBouquetName) {
		System.out.println("TEST USER BOUQET NAME:" + userBouquetName); // cath testing TODO: DELETE

		PreparedStatement pstmt = null;
		String SQL = "SELECT * FROM Bouquet WHERE bName = ?";
		Scanner scanner = new Scanner(System.in);
		int numLeft = 0;
		boolean hasResult = false;
		int bID = 0;
		int pricePaid = 0;
		while(!hasResult) {
			try {
				pstmt = conn.prepareStatement(SQL);
				pstmt.setString(1, userBouquetName);
				ResultSet rs = pstmt.executeQuery();
				hasResult = rs.next();
				numLeft = rs.getInt(3);
				bID = rs.getInt(6);
				pricePaid = rs.getInt(1);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			//check bouquet availability. if dont exist/out of stock, re prompt
			//use prepared statement bc we want to take user input
			if(!hasResult) {
				System.out.println("Unfortunately that bouquet is not available. Please enter a different name");
				userBouquetName = scanner.nextLine();
				continue;
			} else if(numLeft <= 0) {//if bouquet has 0 stock, go back to the orderBouquet() method
				System.out.println(userBouquetName + " is out of stock. Let's order a different one.");
				orderBouquet(conn);
				return;
			}
			boolean isVase = false;
			boolean invalidInput = true;
			String input = "";
			while(invalidInput) {
				System.out.println(userBouquetName + " is available! Would you like the bouquet to go or with a vase?\n"
						+ "Enter 'X' for to go or 'Y' for vase");
				input = scanner.nextLine();
				
				input = input.toLowerCase();
				if(input.equals("x")) { // to go
					invalidInput = false;
					isVase = false;
					
				} else if(input.equals("y")) { // vase
					invalidInput = false;
					isVase = true;
				}else {
					System.out.println("Invalid input. Trying again...");
					continue;
				}
				System.out.println("What is your name?");
				input = scanner.next();
			}
			
			// getting cID from name
			int cID = 0;
			SQL = "SELECT * FROM Customer WHERE cName = ?";
			boolean hasResults = true;
			Statement statement = null;
			ResultSet rs = null;
			try {
				pstmt = conn.prepareStatement(SQL);
				pstmt.setString(1, input);
				rs = pstmt.executeQuery();
				hasResults = rs.isBeforeFirst();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			// Customer does not exist in customer schema
			if(!hasResults) {
				int maxCID = 0;
				try {
					statement = conn.createStatement();
					rs = statement.executeQuery("Select max(cID) from Customer");
					rs.next();
					maxCID = rs.getInt(1);
					maxCID++;
					cID = maxCID;
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}else { // customer exists
				try {
					rs.next();
					cID = rs.getInt(1);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			System.out.println("cID: " + cID + " bID: " + bID + " vase: " + (isVase ? "vase" : "to go"));
			//CALL INSERT TO SALE
			//insertIntoSale(conn, cID, bID, pricePaid, packaging);
			insertIntoSale(conn, cID, bID, pricePaid, isVase ? "vase" : "to go");
			
		}
	}

	//called from orderBouquet()
	private static void createNewBouquetType(Connection conn) {
		System.out.println("Great! I could make your very own, unique bouquet. "
				+ "What flower do you want for your bouquet?");
		Statement stmt1 = null;
		Statement stmt2 = null;
		PreparedStatement pstmt = null;
		try {
			Scanner scanner = new Scanner(System.in); 
			String userInputFlower = "";

			while(scanner.hasNextLine()) {
				// Get user input
				userInputFlower = scanner.nextLine();
				
				
				// Check  if the user input actually already exists/not
				stmt1 = conn.createStatement();
				ResultSet rs = stmt1.executeQuery("Select fName from Flower");

				boolean isUnique = true; // will be changed to true everytime restart while loop

				while(rs.next()) { // check if the flower is NOT unique
					String currFlower = rs.getString("fName");
					if(currFlower.equals(userInputFlower)) { //user is trying to add an existing flower
						isUnique = false;

						System.out.println("We already have a flower of that type! "
								+ "Enter \"A\" if you want an existing bouquet using " + userInputFlower);
						System.out.println("Or enter any other key if you want to re-enter your input.");

						String userChoice = scanner.nextLine();

						if(userChoice.equals("A")) { // use existing bouquet
							buyBouquet(conn, userInputFlower + " Bouquet");
							return;
						} else {
							System.out.println("You wanted to re-enter your input. Please enter the new type of flower you want for your bouquet");
							break;
						}
					}
				}

				if(isUnique) {
					// if reach this point, flower IS UNIQUE

					System.out.println("What color would you like?");
					String userColor = scanner.nextLine();

					int price = (new Random().nextInt(5+20)) + 5; // random price

					//retrieve highest current fID
					stmt2 = conn.createStatement();
					rs = stmt2.executeQuery("select max(fID) as fIDMAX from Flower");
					rs.next();
					int newfID = rs.getInt(1) + 1;
					System.out.println("The current highest fID is " + (newfID - 1) + 
							". So, this new flower will have the fID " + newfID + "!");

					// param: fName, color, fPrice, fID
					pstmt = conn.prepareStatement("insert into Flower values (?, ?, ?, ?)");
					pstmt.setString(1, userInputFlower);
					pstmt.setString(2, userColor);
					pstmt.setInt(3, price);
					pstmt.setInt(4, newfID);
					
					pstmt.executeUpdate(); 

					buyBouquet(conn, userInputFlower + " Bouquet");
				}
			}

		}catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(stmt1!=null) stmt1.close();
				if(stmt2!=null) stmt2.close();
				if(pstmt != null) pstmt.close();
			}catch(SQLException se2){}// nothing we can do

		}

	}

	//called from buyBouquet()
	private static void insertIntoSale(Connection conn, int cID, int bID, int pricePaid, String packaging) {
		PreparedStatement pstmt = null;
		try {
			// Check first if this customer is a discount user.
			if(isCustomerDiscountUser(conn, cID)) {
				pricePaid-= 2; //If they are, reduce the price paid by 2
			} 
			
			//Parameters: (cID, bID, pricePaid, packaging)
			pstmt = conn.prepareStatement("Insert Sale values (?, ?, ?, ?)");
			pstmt.setInt(1, cID);
			pstmt.setInt(2, bID);
			pstmt.setInt(3, pricePaid);
			pstmt.setString(4, packaging);

			pstmt.executeUpdate();

			if(isCustomerDiscountUser(conn, cID)){ // FOR CATH'S TESTING
				System.out.println("success");
			}

			updateBouquetNumCount(conn, bID);
			showCustomerReceipt(conn, cID, bID);	

		} catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(pstmt!=null)
					pstmt.close();
			}catch(SQLException se2){}// nothing we can do

		}

	}

	// pseudocode here. called from insertIntoSale()
	private static void updateBouquetNumCount(Connection conn, int bID) {
		PreparedStatement pstmt = null;
		PreparedStatement pstmt2 = null;
		try {
			pstmt2 = conn.prepareStatement("select numLeft from bouquet where bID = ?");
			pstmt2.setInt(1, bID);
			ResultSet rs = pstmt2.executeQuery();
			while(rs.next()) {
				System.out.println("Currently, we have " + rs.getInt("numLeft") + " of the bouquet in stock.");
			}
			
			String sql = "update bouquet as b1 inner join (select * from bouquet where bID = ?) as b2 using (bID) set b1.numLeft = b2.numLeft - 1 where bID = ?;";
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, bID);
			pstmt.setInt(2, bID);
			pstmt.executeUpdate();
			
			rs = pstmt2.executeQuery();
			while(rs.next()) {
				System.out.println("Thanks to your purchase, now we have " + rs.getInt("numLeft") + " of the bouquet in stock!");
			}
			
			
		} catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(pstmt!=null) pstmt.close();
				if(pstmt2!=null) pstmt2.close();
			}catch(SQLException se2){}// nothing we can do

		}
		
	}

	// pseudocode here. called from updateBouquetNumCount()
	private static void showCustomerReceipt(Connection conn, int cID, int bID) {
		//given the cID and bID, show the customer name (name, not id), bouquet name (name, not bID), pricePaid, and packaging.
		// inner join sale, bouquet, and customer. 
		updatePackaging(conn, cID, bID);
	}

	private static void updatePackaging(Connection conn, int cID, int bID) {
		//prompt user
		PreparedStatement pstmt1 = null;
		PreparedStatement pstmt2 = null;
		try {
			// get the current packaging
			String sql = "select packaging from sale where cID = ? and bId = ?";
			pstmt1 = conn.prepareStatement(sql);
			pstmt1.setInt(1, cID);
			pstmt1.setInt(2, bID);
			
			ResultSet rs = pstmt1.executeQuery();
			String currPackaging = "";
			while(rs.next()) {
				currPackaging = rs.getString("packaging");
			}
			System.out.println("Your current packaging type is " + currPackaging + ".");
			
			// change the packaging
			String sql2 = "";
			if(currPackaging.equals("vase")) {
				sql2 = "update sale set packaging = 'to go' where cID = ? and bId = ?";
				System.out.println("Your packaging has been changed into to go.");
			} else if(currPackaging.equals("to go")) {
				sql2 = "update sale set packaging = 'vase' where cID = ? and bId = ?";
				System.out.println("Your packaging has been changed into vase.");
			}
			pstmt2 = conn.prepareStatement(sql2);
			pstmt2.setInt(1, cID);
			pstmt2.setInt(2, bID);
			pstmt2.executeUpdate();
			
			
		}catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(pstmt1!=null)
					pstmt1.close();
				if(pstmt2!=null)
					pstmt2.close();
			}catch(SQLException se2){}// nothing we can do

		}
		
	}

	private static boolean isCustomerDiscountUser(Connection conn, int cID) throws SQLException {
		CallableStatement cstmt = conn.prepareCall("{call getTotalSpentByCustomerID(?, ?)}"); 

		cstmt.setInt(1, cID); // preparing input
		cstmt.registerOutParameter(2, Types.INTEGER); // register output

		cstmt.executeUpdate();
		int totalCustomerSpent = cstmt.getInt(2);

		if(totalCustomerSpent < 50){ //not a discount user
			return false;
		} else { //is discount user
			return true;
		}	

	}

	private static void viewDiscountEligibility(Connection conn) {
		CallableStatement cstmt = null;
		PreparedStatement pstmt = null;
		try {
			
			// ask for customer name
			System.out.println("Please enter your name so we can get your order history: ");
			Scanner scanner = new Scanner(System.in);
			String name = scanner.nextLine();

			// check if Customer name is in Customer relation
			String SQL = "Select * From Customer Where cName = ?";
			
			pstmt = conn.prepareStatement(SQL);
			pstmt.setString(1, name);
			
			ResultSet rsNameCheck = pstmt.executeQuery(); 
			if(rsNameCheck.next()) {
				
				int cID = rsNameCheck.getInt("cID");
				
				cstmt = conn.prepareCall("{call getTotalSpentByCustomerID(?, ?)}"); 
				cstmt.setInt(1, cID); // preparing input
				cstmt.registerOutParameter(2, Types.INTEGER); // register output

				cstmt.executeUpdate();
				int totalCustomerSpent = cstmt.getInt(2);
				System.out.print("Your total so far is $" + totalCustomerSpent + ".");

				if(totalCustomerSpent < 50){
					System.out.println(" You need to spend $" + (50 - totalCustomerSpent) + 
							" more to be eligible for a discount.");
				} else {
					System.out.println(" You are eligible for a discount!");
				}	
			} else {
				System.out.println("Your name is not in your database. Please place an order. You are eligible for a discount if you spend at least $50.");
			}
			

		}
		catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(cstmt!=null)
					cstmt.close();
			}catch(SQLException se2){
			}// nothing we can do
			try{
				if(conn!=null)
					conn.close();
			}catch(SQLException se){
				se.printStackTrace();
			}

		}
	}

	private static void viewOrderHistory(Connection conn) {

		PreparedStatement pstmt = null;
		try {
			// ask for customer name
			System.out.println("Please enter your name so we can get your order history: ");
			Scanner scanner = new Scanner(System.in);
			String name = "";
			
			while (scanner.hasNextLine())
			{
				name = scanner.nextLine();
				
				// check if Customer name is in Customer relation
				String SQL = "Select * From Customer Where cName = ?";
				
				pstmt = conn.prepareStatement(SQL);
				pstmt.setString(1, name);
				
				ResultSet rsNameCheck = pstmt.executeQuery();  
				System.out.println();
				
				// if the Customer name input is in the customer, then proceed to get their order history in Sale relation
				if (rsNameCheck.next() != false){
					System.out.println("Customer name = " + rsNameCheck.getString("cName") + "; Customer cID = " + rsNameCheck.getInt("cID"));
					// SQL Prepared statement selecting Customer's name, the name of the bouquet, the total price paid and the type of packaging for each order a customer made
					SQL = "Select Customer.cName, Customer.cID, Bouquet.bName, Sale.pricePaid, Sale.packaging From Customer, Sale, Bouquet Where Customer.cName = ? and Customer.cID = Sale.cID and Sale.bID = Bouquet.bID";
					
					pstmt = conn.prepareStatement(SQL);
					pstmt.setString(1, name);
					
					ResultSet rs = pstmt.executeQuery();  
					
					//Display result
					System.out.println("Here are all your previous order history, " + name+ ".");
					System.out.println();
					while(rs.next()){
						System.out.println("Bouquet name = " + rs.getString("Bouquet.bName") + "; Price paid = " + "$"+ rs.getInt("Sale.pricePaid")+ "; Packaging type = " + rs.getString("Sale.packaging"));
					}
					System.out.println();
				}
				
				// if the input name is not in Customer relation, then call viewOrderHistory again to reprompt
				else
				{
					System.out.println("Invalid input. It seems that your name isn't registered in our database as a customer. Please try again.");
					viewOrderHistory(conn);
					
					// Once viewOrderHistory method is done and user wants to go back to main page, it would break the loop
					// it wont execute the commands below if returnMain input is "y" to go back to main page
					break;
				}
				
				// After a customer is done viewing their order history, prompts user if they want to go back to main page
				System.out.println("Would you like to exit out of your order history and return back to the main page? (y/n)");
				if (scanner.hasNextLine()) {
					String returnMain = scanner.nextLine();
					
					if (returnMain.equalsIgnoreCase("y"))
					{
						// returns to main helper prompt
						scanner.close();
						return;
					}
					else
					{
						System.out.println("Please enter your name so we can get your order history: ");
					}
				}
			}


		} catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(pstmt!=null)
					pstmt.close();
			}catch(SQLException se2){}// nothing we can do

		}
		
	}

	private static void viewBouquetInformation(Connection conn) {

		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		System.out.println("These are all of the bouquets we have in stock: ");

		try {
			//FUNCTIONALITY 1: DISPLAY BOUQUET INFO
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select * from Bouquet");

			//Display result
			while(rs.next()){
				System.out.println("Bouquet name = "+rs.getString("bName")+", price = "+rs.getInt("bPrice") + ", stock = " + rs.getInt("numLeft"));
			}

			Scanner scanner = new Scanner(System.in);
			System.out.println("Which flower bouquet would you like to learn more about? Or, enter \"Z\" to do something else.");
			
			while (scanner.hasNextLine())
			{
				String userBouquetSelected = scanner.nextLine();
				if (userBouquetSelected.equals("Z"))
						{
							return;
						}
				else
				{
					//make a sql statement to check if the bouquet exists 
					
					// check if bouquet name inputted is in Bouquet relation
					String SQL = "Select * From Bouquet Where bName = ?";
					pstmt = conn.prepareStatement(SQL);
					
					pstmt.setString(1, userBouquetSelected);
					
					ResultSet rsNameCheck = pstmt.executeQuery();  
					System.out.println();
					
					if (rsNameCheck.next() != false)
					{
						System.out.println("Bouquet name = "+rsNameCheck.getString("bName")+", price = "+rsNameCheck.getInt("bPrice") + ", stock = " + rsNameCheck.getInt("numLeft"));
						System.out.println("Enter \"A\" if you are interested in knowing the color of the bouquet's flowers.");
						System.out.println("Enter \"B\" if you are interested in knowing when I last restocked the flowers in this bouquet.");
						System.out.println("Enter \"C\" if you are interested in knowing the number of flowers in this bouquet.");
						System.out.println("Or, enter \"Z\" to do something else.");


						String userResponse = scanner.nextLine();
						if(userResponse.equals("A")) { // bouquet flower color
							viewFlowerColor(conn, userBouquetSelected);
						}else if(userResponse.equals("B")){ // last restocked flower
							viewFlowerLastRestocked(conn, userBouquetSelected);
						}else if(userResponse.equals("C")) { // num flowers in bouquet
							viewFlowerAmountInBouquet(conn, userBouquetSelected);
						} else if(userResponse.equals("Z")) {
							return;
						}else {
							System.out.println("Invalid input. Please re-enter a valid option.");
						}
						
						System.out.println("Which flower bouquet would you like to learn more about? Or, enter \"Z\" to do something else.");
						
					}
					else
					{
						System.out.println("Invalid input. It seems that bouquet name doesn't exist in our system. Please try again.");
						viewBouquetInformation(conn);
						return;
					}
					
				}
			}
			

		}
		catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(stmt!=null)
					stmt.close();
			}catch(SQLException se2){
			}// nothing we can do
		}
	}

	private static void viewFlowerColor(Connection conn, String bouquetName) {
		PreparedStatement pstmt = null;
		try {
			String SQL ="select * from Flower inner join Bouquet using (fID) where bName = ?";
			pstmt = conn.prepareStatement(SQL);
			pstmt.setString(1, bouquetName);
			ResultSet rs = pstmt.executeQuery();  

			//Display result
			while(rs.next()){
				System.out.println("Flower name = "+rs.getString("fName")+", color = "+rs.getString("color"));
			}

		} catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(pstmt!=null)
					pstmt.close();
			}catch(SQLException se2){
			}
		}// nothing we can do
	}

	private static void viewFlowerLastRestocked(Connection conn, String bouquetName) {
		PreparedStatement pstmt = null;
		try {
			String SQL ="select * from Florist inner join Flower using(fID) inner join Bouquet using (fID) where bName = ?";
			pstmt = conn.prepareStatement(SQL);
			pstmt.setString(1, bouquetName);
			ResultSet rs = pstmt.executeQuery();  

			//Display result
			while(rs.next()){
				System.out.println("Flower name = "+rs.getString("fName")+", last restocked = "+rs.getString("restockDate"));
			}

		} catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(pstmt!=null)
					pstmt.close();
			}catch(SQLException se2){
			}
		}// nothing we can do
	}

	private static void viewFlowerAmountInBouquet(Connection conn, String bouquetName) {
		PreparedStatement pstmt = null;
		try {
			String SQL ="select fCount from Bouquet where bName = ?";
			pstmt = conn.prepareStatement(SQL);
			pstmt.setString(1, bouquetName);
			ResultSet rs = pstmt.executeQuery();  

			//Display result
			while(rs.next()){
				System.out.println("Bouquet name = "+bouquetName+", amount of flower = "+rs.getString(1));
			}

		} catch(SQLException se){
			//Handle errors for JDBC
			se.printStackTrace();
		}catch(Exception e){
			//Handle errors for Class.forName
			e.printStackTrace();
		}finally{
			//finally block used to close resources
			try{
				if(pstmt!=null)
					pstmt.close();
			}catch(SQLException se2){
			}
		}// nothing we can do
	}
}//end JDBCExample
