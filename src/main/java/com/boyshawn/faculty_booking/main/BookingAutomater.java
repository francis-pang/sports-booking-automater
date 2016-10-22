/**
 * 
 */
package com.boyshawn.faculty_booking.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.HttpMethod;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.net.ssl.HttpsURLConnection;

/**
 * So what we wanted to do here is that my friend wanted to have a program to help him book a volleyball court once the slot for the courts
 * are open. The plan for this program is to run this automatically at a certain timing.
 * 
 * @author boyshawn
 * 
 */
public class BookingAutomater {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * Utility
		 */
		StringBuilder strBuilder = new StringBuilder();
		BufferedWriter bufWriter;
		final String QUERY_STRING_STARTER = "?";
		final String QUERY_STRING_SEPARATOR = "&";
		
		/*
		 * We will use the command line argument to capture the following information in the sequence:
		 * 1. Email Address
		 * 2. NRIC number
		 * 3. dates the executer are interested to book
		 */
		String emailAddress = args[0];
		String nric = args[1];
		
		// Convert the string to Calendar object
		GregorianCalendar[] bookingDates = new GregorianCalendar[args.length - 2]; 
		
		for(int i = 0; i < args.length - 2; i++){
			String[] dateComponent = args[i + 2].split("-");
			
			bookingDates[i] = new GregorianCalendar();
			bookingDates[i].set(Calendar.YEAR, Integer.parseInt(dateComponent[0]));
			bookingDates[i].set(Calendar.MONTH, Integer.parseInt(dateComponent[1]) - 1);
			bookingDates[i].set(Calendar.DATE, Integer.parseInt(dateComponent[2]));
		}
		
		/*
		 * Since it is in the plan to use a task scheduler to run this at midnight, so we just had to make HTTP request repeatedly to add it
		 * to the booking cart via trial and error.
		 */
		
		boolean bookingIsDone = false;
		
		try {
			for (int i = 7; i < 21 && bookingIsDone == false; i++){
				bookingDates[0].set(Calendar.HOUR, i - 12);
				bookingDates[0].set(Calendar.MINUTE, 0);
				bookingDates[0].set(Calendar.SECOND, 0);
				System.out.println("Processing date " + bookingDates[0].getTime());
				SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				
				URI addToBasketUri = new URI ("https","bookings.sportshub.com.sg","api/basket.php");
				
				
				HttpsURLConnection httpsRequestConnection = (HttpsURLConnection) new URL("https://bookings.sportshub.com.sg/api/basket.php").openConnection();

				// Connection information
				httpsRequestConnection.setRequestMethod(HttpMethod.POST);
				httpsRequestConnection.setDoOutput(true);
				httpsRequestConnection.setInstanceFollowRedirects(true);
				httpsRequestConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				httpsRequestConnection.setRequestProperty("Connection", "keep-alive");
				
				// Booking information
				strBuilder = new StringBuilder();
				strBuilder.append(new URI("action=add").toASCIIString()).append(QUERY_STRING_SEPARATOR)
						  .append("basket_session=0").append(QUERY_STRING_SEPARATOR)
						  .append("event_date=" + URLEncoder.encode(dateFormatter.format(bookingDates[0].getTime()).toString(), "UTF-8")).append(QUERY_STRING_SEPARATOR)
						  .append("event_type=Activity").append(QUERY_STRING_SEPARATOR)
						  .append("facility_id=11").append(QUERY_STRING_SEPARATOR)
						  .append("price=0.00").append(QUERY_STRING_SEPARATOR);
				
				// Figure when is peak when is non-peak
				if (bookingDates[0].get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
					bookingDates[0].get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){	//Weekend
					strBuilder.append("basketype=Peak");
				} 
				else if (bookingDates[0].get(Calendar.HOUR_OF_DAY) >= 18){ //Monday to Friday
					strBuilder.append("basketype=Peak");
				}
				else {
					strBuilder.append("basketype=" + URLEncoder.encode("Non Peak", "UTF-8"));
				}

				bufWriter = new BufferedWriter(new OutputStreamWriter(httpsRequestConnection.getOutputStream()));
				bufWriter.write(strBuilder.toString());
				bufWriter.flush();
				bufWriter.close();
				
				System.out.println("Sending https://bookings.sportshub.com.sg/api/basket.php");
				System.out.println(strBuilder.toString());
				httpsRequestConnection.connect();
				
				if (httpsRequestConnection.getResponseCode() != HttpURLConnection.HTTP_OK){
					System.out.println("Something wrong with connection. " + httpsRequestConnection.getResponseMessage());
					for (Entry<String, List<String>> header : httpsRequestConnection.getHeaderFields().entrySet()) {
					    System.out.println(header.getKey() + "=" + header.getValue());
					}
				}
				else{
					JsonReader jsonReader = Json.createReader(httpsRequestConnection.getInputStream());
					JsonObject response = jsonReader.readObject();
					System.out.println(response.toString());
					httpsRequestConnection.disconnect();
					jsonReader.close();
				
					if (response.get("error") != JsonValue.NULL){
						System.out.println("Booking on " + bookingDates[0].toString() + " is unsuccessful. " + response.toString());
						continue; 
					}
					//Success
					
					String basketSessionId = response.getString("basketSessionId");
					
					// Edit Basket
					httpsRequestConnection = (HttpsURLConnection) new URL("https://bookings.sportshub.com.sg/api/basket.php").openConnection();
					httpsRequestConnection.setRequestMethod(HttpMethod.POST);
					httpsRequestConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					httpsRequestConnection.setDoOutput(true);
					
					strBuilder = new StringBuilder();
					strBuilder
						.append("action=edit_basket").append(QUERY_STRING_SEPARATOR)
						.append("basket_session=" + basketSessionId).append(QUERY_STRING_SEPARATOR)
						.append("event_date=" + URLEncoder.encode(dateFormatter.format(bookingDates[0].getTime()).toString(), "UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("event_type=Activity").append(QUERY_STRING_SEPARATOR)
						.append("faculty_id=11").append(QUERY_STRING_SEPARATOR)
						.append("touristprice=0.00").append(QUERY_STRING_SEPARATOR)
						.append("price=0.00").append(QUERY_STRING_SEPARATOR)
						.append("id_type=Singaporean");
					
					bufWriter = new BufferedWriter(new OutputStreamWriter(httpsRequestConnection.getOutputStream()));
					bufWriter.write(strBuilder.toString());
					bufWriter.flush();
					bufWriter.close();
					
					System.out.println("Sending https://bookings.sportshub.com.sg/api/basket.php");
					System.out.println(strBuilder.toString());
					httpsRequestConnection.connect();
					
					if (httpsRequestConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						System.out.println("Something wrong with connection. " + httpsRequestConnection.getResponseMessage());
						httpsRequestConnection.disconnect();
						continue;
					}
					
					jsonReader = Json.createReader(httpsRequestConnection.getInputStream());
					response = jsonReader.readObject();
					System.out.println(response.toString());
					httpsRequestConnection.disconnect();
					jsonReader.close();
				
					if (response.get("error") != JsonValue.NULL){
						System.out.println("Booking on " + bookingDates[0].toString() + " is unsuccessful. " + response.toString());
						continue; 
					}
					
					httpsRequestConnection.disconnect();
					
					// IC Check
					httpsRequestConnection = (HttpsURLConnection) new URL("https://bookings.sportshub.com.sg/api/basket.php").openConnection();
					httpsRequestConnection.setRequestMethod(HttpMethod.POST);
					httpsRequestConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					httpsRequestConnection.setDoOutput(true);
					
					strBuilder = new StringBuilder();
					strBuilder
						.append("action=ic_check").append(QUERY_STRING_SEPARATOR)
						.append("ic_number=" + nric).append(QUERY_STRING_SEPARATOR)
						.append("basket_session=" + basketSessionId).append(QUERY_STRING_SEPARATOR)
						.append("citizen=Singaporean");
					
					bufWriter = new BufferedWriter(new OutputStreamWriter(httpsRequestConnection.getOutputStream()));
					bufWriter.write(strBuilder.toString());
					bufWriter.flush();
					bufWriter.close();

					System.out.println("Sending https://bookings.sportshub.com.sg/api/basket.php");
					System.out.println(strBuilder.toString());
					httpsRequestConnection.connect();
					
					if (httpsRequestConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						System.out.println("Something wrong with connection. " + httpsRequestConnection.getResponseMessage());
						httpsRequestConnection.disconnect();
						continue;
					}
					
					jsonReader = Json.createReader(httpsRequestConnection.getInputStream());
					response = jsonReader.readObject();
					System.out.println(response.toString());
					httpsRequestConnection.disconnect();
					jsonReader.close();
				
					if (response.get("error") != JsonValue.NULL){
						System.out.println("Booking on " + bookingDates[0].toString() + " is unsuccessful. " + response.toString());
						continue; 
					}
					
					httpsRequestConnection.disconnect();

					//Get txn id
					// Build GET Request
					
					strBuilder = new StringBuilder();
					strBuilder.append("https://bookings.sportshub.com.sg/api/md5.php");
					strBuilder.append(QUERY_STRING_STARTER);
					strBuilder.append("amount" + "=" + "0");
					strBuilder.append(QUERY_STRING_SEPARATOR);
					strBuilder.append("basket_id" + "=" + basketSessionId);
					
					httpsRequestConnection = (HttpsURLConnection) new URL(strBuilder.toString()).openConnection();
					httpsRequestConnection.setRequestMethod(HttpMethod.GET);
					
					System.out.println("Sending " + strBuilder.toString());
					httpsRequestConnection.connect();
					if (httpsRequestConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						System.out.println("Something wrong with connection. " + httpsRequestConnection.getResponseMessage());
						httpsRequestConnection.disconnect();
						continue;
					}
					
					jsonReader = Json.createReader(httpsRequestConnection.getInputStream());
					response = jsonReader.readObject();
					System.out.println(response.toString());
					jsonReader.close();
					httpsRequestConnection.disconnect();
					
					String txn = response.getString("txn");
					
					//Proceed to book
					httpsRequestConnection = (HttpsURLConnection) new URL("https://bookings.sportshub.com.sg/api/return.php").openConnection();
					httpsRequestConnection.setRequestMethod(HttpMethod.POST);
					httpsRequestConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					httpsRequestConnection.setDoOutput(true);
					
					strBuilder = new StringBuilder();
					strBuilder
						.append("MERCHANT_ACC_NO=8704620149000652").append(QUERY_STRING_SEPARATOR)
						.append("AMOUNT=0.00").append(QUERY_STRING_SEPARATOR)
						.append("TRANSACTION_TYPE=2").append(QUERY_STRING_SEPARATOR)
						.append("MERCHANT_TRANID=" + basketSessionId).append(QUERY_STRING_SEPARATOR)
						.append("RESPONSE_TYPE=HTTP").append(QUERY_STRING_SEPARATOR)
						.append("RETURN_URL=" + URLEncoder.encode("https://bookings.sportshub.com.sg/api/return.php","UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("TXN_DESC=" + URLEncoder.encode("Sports Hub Online Booking","UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("CUSTOMER_ID=").append(QUERY_STRING_SEPARATOR)
						.append("FR_HIGHRISK_EMAIL="+ URLEncoder.encode("abc@mail.com", "UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("FR_HIGHRISK_COUNTRY=" + URLEncoder.encode("United States", "UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("FR_BILLING_ADDRESS=" + URLEncoder.encode("2112, Dome Street", "UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("FR_SHIPPING_ADDRESS=" + URLEncoder.encode("2112, Dome Street", "UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("FR_SHIPPING_COST=10.50").append(QUERY_STRING_SEPARATOR)
						.append("FR_PURCHASE_HOUR=1020").append(QUERY_STRING_SEPARATOR)
						.append("FR_CUSTOMER_IP=211.2.53.164").append(QUERY_STRING_SEPARATOR)
						.append("TXN_SIGNATURE=" + txn).append(QUERY_STRING_SEPARATOR)
						.append("FIRST_NAME=Auto").append(QUERY_STRING_SEPARATOR)
						.append("SURNAME=Program").append(QUERY_STRING_SEPARATOR)
						.append("CUSTOMER_EMAIL=" + URLEncoder.encode(emailAddress, "UTF-8")).append(QUERY_STRING_SEPARATOR)
						.append("TELEPHONE=12345678").append(QUERY_STRING_SEPARATOR)
						.append("IC_NUMBER=" + nric).append(QUERY_STRING_SEPARATOR)
						.append("PARTICIPATE=Singaporean");
						
					bufWriter = new BufferedWriter(new OutputStreamWriter(httpsRequestConnection.getOutputStream()));
					bufWriter.write(strBuilder.toString());
					bufWriter.flush();
					bufWriter.close();

					System.out.println("Sending https://bookings.sportshub.com.sg/api/return.php");
					System.out.println(strBuilder.toString());
					httpsRequestConnection.connect();
					
					if (httpsRequestConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						System.out.println("Something wrong with connection. " + httpsRequestConnection.getResponseMessage());
						httpsRequestConnection.disconnect();
						continue;
					}
					httpsRequestConnection.disconnect();
					
					// Confirm booking
					strBuilder = new StringBuilder();
					strBuilder.append("https://bookings.sportshub.com.sg/confirm.php");
					strBuilder.append(QUERY_STRING_STARTER);
					strBuilder.append("basket_id" + "=" + basketSessionId);
					
					httpsRequestConnection = (HttpsURLConnection) new URL(strBuilder.toString()).openConnection();
					httpsRequestConnection.setRequestMethod(HttpMethod.GET);
					httpsRequestConnection.connect();
					
					httpsRequestConnection.connect();
					if (httpsRequestConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
						System.out.println("Something wrong with connection. " + httpsRequestConnection.getResponseMessage());
						httpsRequestConnection.disconnect();
						continue;
					}
					
					//TODO: Use a JSoup selector
					BufferedReader bufReader = new BufferedReader(new InputStreamReader(httpsRequestConnection.getInputStream()));
					String line;
					strBuilder = new StringBuilder();
					while ((line = bufReader.readLine()) != null){
						strBuilder.append(line);
					}
					
					if (strBuilder.toString().contains("Community Outdoor Facilities - Beach Volleyball")){
						System.out.println("Booked " + dateFormatter.format(bookingDates[0].getTime()).toString());
						bookingIsDone = true;
					}
					else {
						System.out.println(strBuilder.toString());
						System.out.println("Booking fails.");
						System.exit(-1);
					}
				}
			}
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
