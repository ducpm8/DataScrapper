package com.web.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import com.web.entity.*;
import com.web.util.CSVUtils;
import com.web.util.FileUpload;

import org.springframework.web.multipart.MultipartFile;

import com.web.util.config.CommonProperties;
import com.web.data.DataGrab;
import com.web.data.DataGrabP2;
import com.web.data.DataGrabP3;
import com.web.data.DataGrabP4;
import com.web.data.P2SubmitData;

@Controller
@RequestMapping(value = "/home")
public class HelloController {
	
	public DataGrab dataGrab;
	public DataGrabP2 dataGrabP2;
	public DataGrabP3 dataGrabP3;
	public DataGrabP4 dataGrabP4;
	
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String printWelcome(ModelMap model) throws IOException, URISyntaxException {
		
		CommonProperties.loadProperties();
		return "hello";
	}
	
	@RequestMapping(value = "/pj4", method = RequestMethod.GET)
	public String project4(ModelMap model) {
		return "project4";
	}
	
	@RequestMapping(value = "/pj3", method = RequestMethod.GET)
	public String project3(ModelMap model) {
		return "project3";
	}
	
	@RequestMapping(value = "/pj2", method = RequestMethod.GET)
	public String project2(ModelMap model) {
		return "project2";
	}
	
	@RequestMapping(value = "/pj1", method = RequestMethod.GET)
	public String project1(ModelMap model) {
		return "project1";
	}

	@RequestMapping(value = "/hello/{name:.+}", method = RequestMethod.GET)
	public ModelAndView hello(@PathVariable("name") String name) {

		ModelAndView model = new ModelAndView();
		model.setViewName("hello");
		model.addObject("msg", name);

		return model;

	}
	
	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
	public @ResponseBody String uploadFileHandler(HttpServletRequest request, HttpServletResponse response, @ModelAttribute P2SubmitData submitData) {
		
		if (!submitData.getFile().isEmpty()) {
			if (submitData.getProjectId().equals("1")){
				
				try {
					byte[] bytes = submitData.getFile().getBytes();
	
					// Creating the directory to store file
					String rootPath = System.getProperty("catalina.home");
					File dir = new File(rootPath + File.separator + "tmpFiles");
					if (!dir.exists())
						dir.mkdirs();
					
					String filePath = "";
					
					filePath = dir.getAbsolutePath() + File.separator + submitData.getFile().getOriginalFilename();
	
					// Create the file on server
					File serverFile = new File(filePath);
					BufferedOutputStream stream = new BufferedOutputStream(
							new FileOutputStream(serverFile));
					stream.write(bytes);
					stream.close();
		
					Map<String, List> result = new HashMap<String, List>();
					
					List<AmazonInventoryReport> dto = new ArrayList<AmazonInventoryReport>();
					
					dataGrab = new DataGrab();
					
					result = dataGrab.doPullPrice(filePath, submitData.getSheetName());
					
					dto = result.get("SUCCESS");
					
					//Write to CSV file
					String csvFile = dir.getAbsolutePath() + File.separator +  result.get("SHEET").get(0)  + ".csv";
			        FileWriter writer = new FileWriter(csvFile);
			        
			        CSVUtils.writeLine(writer, Arrays.asList("SKU ","Assigned Amazon Sku","ASIN","Cost From AE","Shipping Cost (Epacket)","Amazon Fee","COGS","Margin Target (%)","Margin Target ($)","Amz Listing Price Min","Min Absolute Margin","Suspect List Price","Absolute List Price","Pull Numbers","URL"));
			        
			        if (dto != null) {
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getProductID(), dto.get(i).getAssignedAmazonSku(), dto.get(i).getASIN(), dto.get(i).getCostFromAE(), dto.get(i).getShippingCostEpacket(), dto.get(i).getAmazonFee(), dto.get(i).getCOGS(), dto.get(i).getMarginTargetPercent(), dto.get(i).getMarginTargetDolar(), dto.get(i).getAmzListingPriceMin(), dto.get(i).getMinAbsoluteMargin(), dto.get(i).getSuspectListPrice(), dto.get(i).getAbsoluteListPrice(), dto.get(i).getLine(), dto.get(i).getURL()), ',', '"');
				        }
			        }
	
			        writer.flush();
			        writer.close();
			        
			        //Download
			        File fileDown = new File(csvFile);
					if(!fileDown.exists()){
						throw new ServletException("File doesn't exists on server.");
					}
					System.out.println("File location on server::" + fileDown.getAbsolutePath());
					//ServletContext ctx = getServletContext();
					InputStream fis = new FileInputStream(fileDown);
					//String mimeType = ctx.getMimeType(fileDown.getAbsolutePath());
					response.setContentType("application/octet-stream");
					response.setContentLength((int) fileDown.length());
					response.setHeader("Content-Disposition", "attachment; filename=\"" + result.get("SHEET").get(0)  + ".csv" + "\"");
	
					ServletOutputStream os = response.getOutputStream();
					byte[] bufferData = new byte[1024];
					int read=0;
					while((read = fis.read(bufferData))!= -1){
						os.write(bufferData, 0, read);
					}
					os.flush();
					os.close();
					fis.close();
					
					dir = new File(rootPath + File.separator + "tmpFiles" + File.separator + "Fail");
					if (!dir.exists())
						dir.mkdirs();
			        
			        dto = new ArrayList<AmazonInventoryReport>();
			        
			        dto = result.get("FAIL");
			        
			        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HHmmss");//dd/MM/yyyy
			        Date now = new Date();
			        String strDate = sdfDate.format(now);
			        
			        if (dto != null && dto.size() > 0) {
			        	csvFile = dir.getAbsolutePath() + File.separator + "Fail_" + strDate + ".csv";
				        writer = new FileWriter(csvFile);
				        
				        CSVUtils.writeLine(writer, Arrays.asList("SKU ","ASIN","URL","Reason","Input Line"));
				        
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getProductID(), dto.get(i).getASIN(), dto.get(i).getURL(), dto.get(i).getReason(), dto.get(i).getLine()), ',', '"');
				        }
				        
				        writer.flush();
				        writer.close();
			        	
			        }
			        
			        try {
				        boolean delResult = Files.deleteIfExists(fileDown.toPath()); 
				        delResult = Files.deleteIfExists(serverFile.toPath());
			        } catch (IOException ioe) {
			        	System.out.println("Can not delete file." + ioe);
			        	return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
			        }
	
					return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
				} catch (Exception e) {
					return "You failed to upload " + submitData.getFile().getOriginalFilename() + " => " + e.getMessage();
				}
			} else if (submitData.getProjectId().equals("2")) {
				
				try {
					byte[] bytes = submitData.getFile().getBytes();
	
					// Creating the directory to store file
					String rootPath = System.getProperty("catalina.home");
					File dir = new File(rootPath + File.separator + "tmpFiles");
					if (!dir.exists())
						dir.mkdirs();
					
					String filePath = "";
					
					filePath = dir.getAbsolutePath() + File.separator + submitData.getFile().getOriginalFilename();
	
					// Create the file on server
					File serverFile = new File(filePath);
					BufferedOutputStream stream = new BufferedOutputStream(
							new FileOutputStream(serverFile));
					stream.write(bytes);
					stream.close();
	
					Map<String, List> result = new HashMap<String, List>();
					
					List<AmazonOrderReport> dto = new ArrayList<AmazonOrderReport>();
					
					dataGrabP2 = new DataGrabP2();
					
					result = dataGrabP2.doPlaceOrder(filePath, submitData);
					
					dto = result.get("SUCCESS");
					
					//Write to CSV file
					String csvFile = dir.getAbsolutePath() + File.separator +  result.get("SHEET").get(0)  + ".csv";
			        FileWriter writer = new FileWriter(csvFile);
			        
			        CSVUtils.writeLine(writer, Arrays.asList("order-id",
			        		"order-item-id",
			        		"Product Name",
			        		"Total Pruchase Price",
			        		"Total Customer Spend on Order",
			        		"Margin","Margin (%)",
			        		"Vendor Order number",
			        		"Tracking Number",
			        		"payments-date",
			        		"purchase-date",
			        		"reporting-date",
			        		"promise-date",
			        		"days-past-promise",
			        		"buyer-email",
			        		"buyer-name",
			        		"buyer-phone-number",
			        		"sku",
			        		"product-name",
			        		"quantity-purchased",
			        		"quantity-shipped",
			        		"quantity-to-ship",
			        		"ship-service-level",
			        		"recipient-name",
			        		"ship-address-1",
			        		"ship-address-2",
			        		"ship-address-3",
			        		"ship-city",
			        		"ship-state",
			        		"ship-postal-code",
			        		"ship-country"));
			        
			        if (dto != null) {
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getOrderId(), 
				        			dto.get(i).getOrderItemId(),
				        			dto.get(i).getProductName(),
				        			dto.get(i).getTotalPruchasePrice(),
				        			dto.get(i).getTotalCustomerSpendonOrder(),
				        			dto.get(i).getMarginDollar(),
				        			dto.get(i).getMarginPercent(),
				        			dto.get(i).getVendorOrdernumber(),
				        			dto.get(i).getTrackingNumber(),
				        			dto.get(i).getPaymentsDate(),
				        			dto.get(i).getPurchaseDate(),
				        			dto.get(i).getReportingDate(),
				        			dto.get(i).getPromiseDate(),
				        			dto.get(i).getDaysPastPromise(),
				        			dto.get(i).getBuyerEmail(),
				        			dto.get(i).getBuyerName(),
				        			dto.get(i).getBuyerPhoneNumber(),
				        			dto.get(i).getSku(),
				        			dto.get(i).getProductName2(),
				        			dto.get(i).getQuantityPurchased(),
				        			dto.get(i).getQuantityShipped(),
				        			dto.get(i).getQuantityToShip(),
				        			dto.get(i).getShipServiceLevel(),
				        			dto.get(i).getRecipientName(),
				        			dto.get(i).getShipAddress1(),
				        			dto.get(i).getShipAddress2(),
				        			dto.get(i).getShipAddress3(),
				        			dto.get(i).getShipCity(),
				        			dto.get(i).getShipState(),
				        			dto.get(i).getShipPostalCode(),
				        			dto.get(i).getShipCountry()), ',', '"');
				        }
			        }
	
			        writer.flush();
			        writer.close();
			        
			        //Download
			        File fileDown = new File(csvFile);
					if(!fileDown.exists()){
						throw new ServletException("File doesn't exists on server.");
					}
					System.out.println("File location on server::" + fileDown.getAbsolutePath());
					InputStream fis = new FileInputStream(fileDown);
					response.setContentType("application/octet-stream");
					response.setContentLength((int) fileDown.length());
					response.setHeader("Content-Disposition", "attachment; filename=\"" + result.get("SHEET").get(0)  + ".csv" + "\"");
	
					ServletOutputStream os = response.getOutputStream();
					byte[] bufferData = new byte[1024];
					int read=0;
					while((read = fis.read(bufferData))!= -1){
						os.write(bufferData, 0, read);
					}
					os.flush();
					os.close();
					fis.close();
					
					dir = new File(rootPath + File.separator + "tmpFiles" + File.separator + "Fail");
					if (!dir.exists())
						dir.mkdirs();
			        
			        dto = new ArrayList<AmazonOrderReport>();
			        
			        dto = result.get("FAIL");
			        
			        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HHmmss");//dd/MM/yyyy
			        Date now = new Date();
			        String strDate = sdfDate.format(now);
			        
			        if (dto != null && dto.size() > 0) {
			        	csvFile = dir.getAbsolutePath() + File.separator + "Fail_" + strDate + ".csv";
				        writer = new FileWriter(csvFile);
				        
				        CSVUtils.writeLine(writer, Arrays.asList("SKU ","Product Name","URL","Reason","Input Line"));
				        
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getSku(),dto.get(i).getProductName(), dto.get(i).getURL(), dto.get(i).getReason(), dto.get(i).getLine()), ',', '"');
				        }
				        
				        writer.flush();
				        writer.close();
			        	
			        }
			        
			        try {
				        boolean delResult = Files.deleteIfExists(fileDown.toPath()); 
				        delResult = Files.deleteIfExists(serverFile.toPath());
			        } catch (IOException ioe) {
			        	System.out.println("Can not delete file." + ioe);
			        	return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
			        }
	
					return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
				} catch (Exception e) {
					return "You failed to upload " + submitData.getFile().getOriginalFilename() + " => " + e.getMessage();
				}
				
			}
		} else {
			return "You failed to upload " + submitData.getFile().getOriginalFilename()
					+ " because the file was empty.";
		}
		
		return "OK";
	}
	
	@RequestMapping(value = "/uploadFileP3", method = RequestMethod.POST)
	public @ResponseBody String uploadFileHandlerP3(HttpServletRequest request, HttpServletResponse response, @ModelAttribute P2SubmitData submitData) {
		if (!submitData.getFile().isEmpty()) {
			if (submitData.getProjectId().equals("3")) {
				
				try {
					byte[] bytes = submitData.getFile().getBytes();
	
					// Creating the directory to store file
					String rootPath = System.getProperty("catalina.home");
					File dir = new File(rootPath + File.separator + "tmpFiles");
					if (!dir.exists())
						dir.mkdirs();
					
					String filePath = "";
					
					filePath = dir.getAbsolutePath() + File.separator + submitData.getFile().getOriginalFilename();
	
					// Create the file on server
					File serverFile = new File(filePath);
					BufferedOutputStream stream = new BufferedOutputStream(
							new FileOutputStream(serverFile));
					stream.write(bytes);
					stream.close();
	
					Map<String, List> result = new HashMap<String, List>();
					
					List<AmazonOrderReport> dto = new ArrayList<AmazonOrderReport>();
					
					dataGrabP3 = new DataGrabP3();
					
					result = dataGrabP3.doCollectTrackingNo(filePath, submitData);
					
					dto = result.get("SUCCESS");
					
					//Write to CSV file
					String csvFile = dir.getAbsolutePath() + File.separator +  result.get("SHEET").get(0)  + ".csv";
			        FileWriter writer = new FileWriter(csvFile);
			        
			        CSVUtils.writeLine(writer, Arrays.asList("order-id",
			        		"order-item-id",
			        		"Product Name",
			        		"Total Pruchase Price",
			        		"Total Customer Spend on Order",
			        		"Margin","Margin (%)",
			        		"Vendor Order number",
			        		"Tracking Number",
			        		"payments-date",
			        		"purchase-date",
			        		"reporting-date",
			        		"promise-date",
			        		"days-past-promise",
			        		"buyer-email",
			        		"buyer-name",
			        		"buyer-phone-number",
			        		"sku",
			        		"product-name",
			        		"quantity-purchased",
			        		"quantity-shipped",
			        		"quantity-to-ship",
			        		"ship-service-level",
			        		"recipient-name",
			        		"ship-address-1",
			        		"ship-address-2",
			        		"ship-address-3",
			        		"ship-city",
			        		"ship-state",
			        		"ship-postal-code",
			        		"ship-country"));
			        
			        if (dto != null) {
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getOrderId(), 
				        			dto.get(i).getOrderItemId(),
				        			dto.get(i).getProductName(),
				        			dto.get(i).getTotalPruchasePrice(),
				        			dto.get(i).getTotalCustomerSpendonOrder(),
				        			dto.get(i).getMarginDollar(),
				        			dto.get(i).getMarginPercent(),
				        			dto.get(i).getVendorOrdernumber(),
				        			dto.get(i).getTrackingNumber(),
				        			dto.get(i).getPaymentsDate(),
				        			dto.get(i).getPurchaseDate(),
				        			dto.get(i).getReportingDate(),
				        			dto.get(i).getPromiseDate(),
				        			dto.get(i).getDaysPastPromise(),
				        			dto.get(i).getBuyerEmail(),
				        			dto.get(i).getBuyerName(),
				        			dto.get(i).getBuyerPhoneNumber(),
				        			dto.get(i).getSku(),
				        			dto.get(i).getProductName2(),
				        			dto.get(i).getQuantityPurchased(),
				        			dto.get(i).getQuantityShipped(),
				        			dto.get(i).getQuantityToShip(),
				        			dto.get(i).getShipServiceLevel(),
				        			dto.get(i).getRecipientName(),
				        			dto.get(i).getShipAddress1(),
				        			dto.get(i).getShipAddress2(),
				        			dto.get(i).getShipAddress3(),
				        			dto.get(i).getShipCity(),
				        			dto.get(i).getShipState(),
				        			dto.get(i).getShipPostalCode(),
				        			dto.get(i).getShipCountry()), ',', '"');
				        }
			        }
	
			        writer.flush();
			        writer.close();
			        
			        //Download
			        File fileDown = new File(csvFile);
					if(!fileDown.exists()){
						throw new ServletException("File doesn't exists on server.");
					}
					System.out.println("File location on server::" + fileDown.getAbsolutePath());
					//ServletContext ctx = getServletContext();
					InputStream fis = new FileInputStream(fileDown);
					//String mimeType = ctx.getMimeType(fileDown.getAbsolutePath());
					response.setContentType("application/octet-stream");
					response.setContentLength((int) fileDown.length());
					response.setHeader("Content-Disposition", "attachment; filename=\"" + result.get("SHEET").get(0)  + ".csv" + "\"");
	
					ServletOutputStream os = response.getOutputStream();
					byte[] bufferData = new byte[1024];
					int read=0;
					while((read = fis.read(bufferData))!= -1){
						os.write(bufferData, 0, read);
					}
					os.flush();
					os.close();
					fis.close();
					
					dir = new File(rootPath + File.separator + "tmpFiles" + File.separator + "Fail");
					if (!dir.exists())
						dir.mkdirs();
			        
			        dto = new ArrayList<AmazonOrderReport>();
			        
			        dto = result.get("FAIL");
			        
			        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HHmmss");//dd/MM/yyyy
			        Date now = new Date();
			        String strDate = sdfDate.format(now);
			        
			        if (dto != null && dto.size() > 0) {
			        	csvFile = dir.getAbsolutePath() + File.separator + "Fail_" + strDate + ".csv";
				        writer = new FileWriter(csvFile);
				        
				        CSVUtils.writeLine(writer, Arrays.asList("Order Number","URL","Reason","Input Line"));
				        
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getVendorOrdernumber(), dto.get(i).getURL(), dto.get(i).getReason(), dto.get(i).getLine()), ',', '"');
				        }
				        
				        writer.flush();
				        writer.close();
			        	
			        }
			        
			        try {
				        boolean delResult = Files.deleteIfExists(fileDown.toPath()); 
				        delResult = Files.deleteIfExists(serverFile.toPath());
			        } catch (IOException ioe) {
			        	System.out.println("Can not delete file." + ioe);
			        	return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
			        }
	
					return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
				} catch (Exception e) {
					return "You failed to upload " + submitData.getFile().getOriginalFilename() + " => " + e.getMessage();
				}
				
			}
		} else {
			return "You failed to upload " + submitData.getFile().getOriginalFilename()
					+ " because the file was empty.";
		}
		
		return "OK";
	}
	
	@RequestMapping(value = "/uploadFileP4", method = RequestMethod.POST)
	public @ResponseBody String uploadFileHandlerP4(HttpServletRequest request, HttpServletResponse response, @ModelAttribute P2SubmitData submitData) {
		if (!submitData.getFile().isEmpty()) {
			if (submitData.getProjectId().equals("3")) {
				
				try {
					byte[] bytes = submitData.getFile().getBytes();
	
					// Creating the directory to store file
					String rootPath = System.getProperty("catalina.home");
					File dir = new File(rootPath + File.separator + "tmpFiles");
					if (!dir.exists())
						dir.mkdirs();
					
					String filePath = "";
					
					filePath = dir.getAbsolutePath() + File.separator + submitData.getFile().getOriginalFilename();
	
					// Create the file on server
					File serverFile = new File(filePath);
					BufferedOutputStream stream = new BufferedOutputStream(
							new FileOutputStream(serverFile));
					stream.write(bytes);
					stream.close();
	
					Map<String, List> result = new HashMap<String, List>();
					
					List<AmazonOrderReport> dto = new ArrayList<AmazonOrderReport>();
					
					dataGrabP4 = new DataGrabP4();
					
					result = dataGrabP4.doCollectTrackingDetail(filePath, submitData);
					
					dto = result.get("SUCCESS");
					
					//Write to CSV file
					String csvFile = dir.getAbsolutePath() + File.separator +  result.get("SHEET").get(0)  + ".csv";
			        FileWriter writer = new FileWriter(csvFile);
			        
			        CSVUtils.writeLine(writer, Arrays.asList("order-id",
			        		"order-item-id",
			        		"Product Name",
			        		"Total Pruchase Price",
			        		"Total Customer Spend on Order",
			        		"Margin","Margin (%)",
			        		"Vendor Order number",
			        		"Tracking Number",
			        		"payments-date",
			        		"purchase-date",
			        		"reporting-date",
			        		"promise-date",
			        		"days-past-promise",
			        		"buyer-email",
			        		"buyer-name",
			        		"buyer-phone-number",
			        		"sku",
			        		"product-name",
			        		"quantity-purchased",
			        		"quantity-shipped",
			        		"quantity-to-ship",
			        		"ship-service-level",
			        		"recipient-name",
			        		"ship-address-1",
			        		"ship-address-2",
			        		"ship-address-3",
			        		"ship-city",
			        		"ship-state",
			        		"ship-postal-code",
			        		"ship-country",
			        		"ShipDetailDateTime",
			        		"Email Content"));
			        
			        if (dto != null) {
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getOrderId(), 
				        			dto.get(i).getOrderItemId(),
				        			dto.get(i).getProductName(),
				        			dto.get(i).getTotalPruchasePrice(),
				        			dto.get(i).getTotalCustomerSpendonOrder(),
				        			dto.get(i).getMarginDollar(),
				        			dto.get(i).getMarginPercent(),
				        			dto.get(i).getVendorOrdernumber(),
				        			dto.get(i).getTrackingNumber(),
				        			dto.get(i).getPaymentsDate(),
				        			dto.get(i).getPurchaseDate(),
				        			dto.get(i).getReportingDate(),
				        			dto.get(i).getPromiseDate(),
				        			dto.get(i).getDaysPastPromise(),
				        			dto.get(i).getBuyerEmail(),
				        			dto.get(i).getBuyerName(),
				        			dto.get(i).getBuyerPhoneNumber(),
				        			dto.get(i).getSku(),
				        			dto.get(i).getProductName2(),
				        			dto.get(i).getQuantityPurchased(),
				        			dto.get(i).getQuantityShipped(),
				        			dto.get(i).getQuantityToShip(),
				        			dto.get(i).getShipServiceLevel(),
				        			dto.get(i).getRecipientName(),
				        			dto.get(i).getShipAddress1(),
				        			dto.get(i).getShipAddress2(),
				        			dto.get(i).getShipAddress3(),
				        			dto.get(i).getShipCity(),
				        			dto.get(i).getShipState(),
				        			dto.get(i).getShipPostalCode(),
				        			dto.get(i).getShipCountry(),
				        			dto.get(i).getShipmentStatusDate(),
				        			dto.get(i).getEmailContent()), ',', '"');
				        }
			        }
	
			        writer.flush();
			        writer.close();
			        
			        //Download
			        File fileDown = new File(csvFile);
					if(!fileDown.exists()){
						throw new ServletException("File doesn't exists on server.");
					}
					System.out.println("File location on server::" + fileDown.getAbsolutePath());
					//ServletContext ctx = getServletContext();
					InputStream fis = new FileInputStream(fileDown);
					//String mimeType = ctx.getMimeType(fileDown.getAbsolutePath());
					response.setContentType("application/octet-stream");
					response.setContentLength((int) fileDown.length());
					response.setHeader("Content-Disposition", "attachment; filename=\"" + result.get("SHEET").get(0)  + ".csv" + "\"");
	
					ServletOutputStream os = response.getOutputStream();
					byte[] bufferData = new byte[1024];
					int read=0;
					while((read = fis.read(bufferData))!= -1){
						os.write(bufferData, 0, read);
					}
					os.flush();
					os.close();
					fis.close();
					
					dir = new File(rootPath + File.separator + "tmpFiles" + File.separator + "Fail");
					if (!dir.exists())
						dir.mkdirs();
			        
			        dto = new ArrayList<AmazonOrderReport>();
			        
			        dto = result.get("FAIL");
			        
			        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HHmmss");//dd/MM/yyyy
			        Date now = new Date();
			        String strDate = sdfDate.format(now);
			        
			        if (dto != null && dto.size() > 0) {
			        	csvFile = dir.getAbsolutePath() + File.separator + "Fail_" + strDate + ".csv";
				        writer = new FileWriter(csvFile);
				        
				        CSVUtils.writeLine(writer, Arrays.asList("Order Number","URL","Reason","Input Line"));
				        
				        for (int i=0; i < dto.size(); i++) {
				        	CSVUtils.writeLine(writer, Arrays.asList(dto.get(i).getVendorOrdernumber(), dto.get(i).getURL(), dto.get(i).getReason(), dto.get(i).getLine()), ',', '"');
				        }
				        
				        writer.flush();
				        writer.close();
			        	
			        }
			        
			        try {
				        boolean delResult = Files.deleteIfExists(fileDown.toPath()); 
				        delResult = Files.deleteIfExists(serverFile.toPath());
			        } catch (IOException ioe) {
			        	System.out.println("Can not delete file." + ioe);
			        	return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
			        }
	
					return "You successfully uploaded file=" + submitData.getFile().getOriginalFilename();
				} catch (Exception e) {
					return "You failed to upload " + submitData.getFile().getOriginalFilename() + " => " + e.getMessage();
				}
				
			}
		} else {
			return "You failed to upload " + submitData.getFile().getOriginalFilename()
					+ " because the file was empty.";
		}
		
		return "OK";
	}
	
	
	@RequestMapping(value = "/trace", method = RequestMethod.GET)
	public ModelAndView traceFail() {
		
		List<String> fileLst = new ArrayList<String>();
		String rootPath = System.getProperty("catalina.home");
		
		final File folder = new File(rootPath + File.separator + "tmpFiles" + File.separator + "Fail");
		fileLst = listFilesForFolder(folder);
		
		ModelAndView model = new ModelAndView();
		model.setViewName("trace");
		//model.addObject("msg", name);
		model.addObject("fileList", fileLst);

		return model;

	}
	
	@RequestMapping(value = "/download/{fileName:.+}", method = RequestMethod.GET)
	public void getErrorTrace(@PathVariable("fileName") String name, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ModelAndView model = new ModelAndView();
		model.setViewName("trace");
		model.addObject("msg", name);
		
		String rootPath = System.getProperty("catalina.home");
		File fileDown = new File(rootPath + File.separator + "tmpFiles" + File.separator + "Fail" + File.separator +  name);
		
		if(!fileDown.exists()){
			throw new ServletException("File doesn't exists on server.");
		}
		System.out.println("File location on server::" + fileDown.getAbsolutePath());
		//ServletContext ctx = getServletContext();
		ServletOutputStream os = null;
		InputStream fis = null;
		try {
			fis = new FileInputStream(fileDown);
			//String mimeType = ctx.getMimeType(fileDown.getAbsolutePath());
			response.setContentType("application/octet-stream");
			response.setContentLength((int) fileDown.length());
			response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
	
			os = response.getOutputStream();
			byte[] bufferData = new byte[1024];
			int read=0;
			while((read = fis.read(bufferData))!= -1){
				os.write(bufferData, 0, read);
			}
			
		} catch (Exception ex) {
			System.out.println("Error");
		} finally {
			os.flush();
			os.close();
			fis.close();
		}

	}
	
	public List<String> listFilesForFolder(final File folder) {
		List<String> fileLst = new ArrayList<String>();
		
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	        	fileLst.add(fileEntry.getName());
	        }
	    }
	    
	    return fileLst;
	}
}