package com.web.data;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor;

import com.web.entity.*;
import com.web.util.AmazonFeeCal;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DataGrab {
	
	private AmazonInventoryReport inventoryReport;
	private AmazonFeeCal amazonCalculate;
	
	public static String homepage = "https://www.aliexpress.com";
	
	public Map<String, List> doPullPrice(String fileName, String sheetName)
    {
		
		Map<String, List> result = new HashMap<String, List>();
		
		List<AmazonInventoryReport> inventoryReportList = new ArrayList<AmazonInventoryReport>();
		
		List<AmazonInventoryReport> inventoryReportFailList = new ArrayList<AmazonInventoryReport>();
		
		amazonCalculate = new AmazonFeeCal();
		
		WebDriver driver = new ChromeDriver();
		
		driver.manage().window().maximize();

        try {
        	
        	String productID_Trim2_SKU = StringUtils.EMPTY;
            String price;
        	char extention;
        	FileInputStream file;
        	
        	file = new FileInputStream(new File(fileName));
        	extention =  fileName.charAt(fileName.length() - 1);
        	if (extention == 'x') {
        		//Get the workbook instance for XLS file 
            	XSSFWorkbook wbs = new XSSFWorkbook (file);
                
                for (int i=0; i < wbs.getNumberOfSheets(); i++) {
            	//for (int i=0; i < 1; i++) {
                	Sheet mySheet = wbs.getSheetAt(i);
                	
                	if (sheetName != null && StringUtils.isNotEmpty(sheetName)) {
                		sheetName = wbs.getSheetAt(i).getSheetName();
                	}
                	
                	if (mySheet.getSheetName().equals(sheetName)) {
                    
	                    Row row;
	                    
	                    for (int j=0; j <= mySheet.getLastRowNum(); j++) {
	                    	inventoryReport = new AmazonInventoryReport();
	                    	
	                    	row = mySheet.getRow(j);
	                    	
	                    	if (row != null) {
		                    	//URL data
		                    	Cell cell = row.getCell(0);
		                    	
		                    	if (cell != null) {
		                    	
			                    	switch (cell.getCellType()) { 
			    	                	
			    	                	case Cell.CELL_TYPE_STRING: 
			    	                		productID_Trim2_SKU = cell.getStringCellValue();
			    	                		
			    	                		driver.navigate().to("https://www.aliexpress.com");
			    	                		
			    	                		//Input productID
			    	                		driver.findElement(By.xpath("//input[contains(@id,'search-key')]")).sendKeys(productID_Trim2_SKU);
			    	                		
			    	                		//Click Search
			    	                		driver.findElement(By.xpath("//input[contains(@class,'search-button')]")).click();
			    	                		
			    	                		
			    	                        price = driver.findElement(By.xpath("//span[contains(@id, 'j-sku-price')]")).getText();
			    	                		
			    	                        cell.setCellValue(price);
			    	                        
			    	                		break; 
			            				default :
			                    	}
		                    	}
	                    	}
	                    }
                	}
                	
                	break;
                }
                file.close();
        	} else {
        		file = new FileInputStream(new File(fileName));
    			
            	//Get the workbook instance for XLS file 
            	HSSFWorkbook workbook = new HSSFWorkbook(file);
            	
            	HSSFRow rowHS;
            	HSSFCell cellHS;
            	HSSFCell cellHS2;
            	
            	driver.navigate().to(homepage);
            	
            	WebElement element;
            	
            	for (int i=0; i < workbook.getNumberOfSheets(); i++) {
            	//for (int i=0; i < 1; i++) {
            		//Get first sheet from the workbook
                	HSSFSheet sheet = workbook.getSheetAt(i);
                	
                	if (sheetName == null || StringUtils.isEmpty(sheetName)) {
                		sheetName = workbook.getSheetAt(i).getSheetName();
                	}
                	
                	if (sheet.getSheetName().equals(sheetName)) {
                	
	                	for (int j=1; j <= sheet.getLastRowNum(); j++) {
	                		inventoryReport = new AmazonInventoryReport();
	                	//for (int j=1; j < 3; j++) {
	                		rowHS = sheet.getRow(j);
	                		if (rowHS != null) {
		                		cellHS = rowHS.getCell(0);
		                		if (cellHS != null) {
		                			
		                			cellHS.setCellType(Cell.CELL_TYPE_STRING);
		                			productID_Trim2_SKU = cellHS.getStringCellValue();
		                			
		                			cellHS2 = rowHS.getCell(1);
		                			
		                			cellHS2.setCellType(Cell.CELL_TYPE_STRING);
		                			
			                		if (StringUtils.isEmpty(productID_Trim2_SKU) && StringUtils.isNotEmpty(cellHS2.getStringCellValue())) {
			                			//Can not obtains SKU in csv input
			                			
			                			inventoryReport.setProductID(StringUtils.EMPTY);
				                        
				                        if(cellHS2 != null) {
				                        	inventoryReport.setASIN(cellHS2.getStringCellValue());
				                        }
			                			
			                			inventoryReport.setURL(StringUtils.EMPTY);
			                			
			                			inventoryReport.setReason("Can not obtains SKU");
			                			inventoryReport.setLine(String.valueOf(j+1));
			                			inventoryReportFailList.add(inventoryReport);
			                			
			                			continue;
			                		} else if (StringUtils.isEmpty(productID_Trim2_SKU) && StringUtils.isEmpty(cellHS2.getStringCellValue())) {
			                			continue;
			                		}
			                		
			                		try {
				                		//Input productID
			                			element = (new WebDriverWait(driver, 10))
				                        		   .until(ExpectedConditions.elementToBeClickable(By.xpath("//input[contains(@id,'search-key')]")));
			                			
			                			element.clear();
			                			element.sendKeys(productID_Trim2_SKU);
		    	                		
		    	                		//Click Search
			                			element = (new WebDriverWait(driver, 10))
				                        		   .until(ExpectedConditions.elementToBeClickable(By.xpath("//input[contains(@class,'search-button')]")));
			                			
			                			element.click();
		    	                		List<WebElement> elems = driver.findElements(By.xpath("//span[contains(@id, 'j-sku-discount-price')]"));
		    	                		
		    	                		if ((elems.size() > 0)) {
		    	                			element = (new WebDriverWait(driver, 10))
					                        		   .until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(@id, 'j-sku-discount-price')]")));
		    	                			
		    	                			price = element.getText();
		    	                		} else {
		    	                			elems = driver.findElements(By.xpath("//span[contains(@id, 'j-sku-price')]"));
		    	                			
		    	                			if (elems.size() > 0) {
		    	                				
		    	                				element = (new WebDriverWait(driver, 10))
						                        		   .until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(@id, 'j-sku-price')]")));
		    	                				
					                			price = element.getText();
					                		} else {
					                			//Product not found
					                			
					                			inventoryReport.setProductID(productID_Trim2_SKU);
					                			
					                			cellHS2.setCellType(Cell.CELL_TYPE_STRING);
						                        if(cellHS2 != null) {
						                        	inventoryReport.setASIN(cellHS2.getStringCellValue());
						                        }
					                			
					                			inventoryReport.setURL(driver.getCurrentUrl());
					                			
					                			inventoryReport.setReason("Product not found");
					                			inventoryReport.setLine(String.valueOf(j+1));
					                			inventoryReportFailList.add(inventoryReport);
					                			
					                			continue;
					                		}
		    	                		}
				                        //Shipping Price
				                        driver.findElement(By.xpath("//div[@id='j-product-tabbed-pane']/ul/li[3]/a")).click();
				                        
				                        element = (new WebDriverWait(driver, 10))
				                        		   .until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id='j-pnl-country-selector']/a")));
				                        
				                        element.click();
				                        element = (new WebDriverWait(driver, 10))
				                        		   .until(ExpectedConditions.elementToBeClickable(By.xpath("//li[@data-code='us']/span")));
				                        
				                        element.click();
				                        
				                        element = (new WebDriverWait(driver, 10))
				                        		   .until(ExpectedConditions.elementToBeClickable(By.xpath("//tr[@data-company-code='EMS_ZX_ZX_US']/td[2]")));
				                        
				                        String jss = "";
				                        String shipment = "";
				                        jss = "var x=document.getElementsByClassName('col-company');var k=document.getElementsByClassName('s-price-detail');var i;for(i=0;i<x.length;i++){if(x[i].innerHTML=='ePacket'){return k[i].innerHTML}}";
				                        
				                        shipment = element.getAttribute("innerHTML");
				                        
				                        //Process Shipping Fee
				                        if (StringUtils.isNotEmpty(shipment) && !(shipment.indexOf("</del>") < 0)) {
				                        	shipment = shipment.substring(shipment.indexOf("</del>") + 6, shipment.length());
				                        }
				                		
				                		shipment = shipment.replace(StringUtils.SPACE, StringUtils.EMPTY);
				                		
				                		if (StringUtils.isNotEmpty(shipment) && !(shipment.indexOf("US$") < 0) && !(shipment.indexOf("</span>") < 0)) {
				                        	shipment = shipment.substring(shipment.indexOf("US$") + 3, shipment.indexOf("</span>"));
				                        }
				                        
				                        if (StringUtils.isNotEmpty(shipment) && shipment.indexOf("Free Shipping") < 0 && shipment.indexOf("FreeShipping") < 0) {
				                        	shipment = StringUtils.replace(shipment,"US $","");
				                        } else {
				                        	shipment = "0";
				                        }
				                        inventoryReport.setShippingCostEpacket(shipment);
				                        
				                        inventoryReport.setMinAbsoluteMargin("3");
				                        inventoryReport.setMarginTargetPercent("50");
				                		
				                        if(cellHS2 != null) {
				                        	inventoryReport.setASIN(cellHS2.getStringCellValue());
				                        }
				                        
				                        inventoryReport.setURL(driver.getCurrentUrl());
				                        inventoryReport.setCostFromAE(price);
				                        inventoryReport.setProductID(productID_Trim2_SKU);
				                        
				                        inventoryReport.setAssignedAmazonSku(productID_Trim2_SKU + "-HF-AE");
				                        inventoryReport.setLine(String.valueOf(j+1));
				                        
				                        //Price calculation
				                        inventoryReport = amazonCalculate.priceCalculate(inventoryReport);
				                        inventoryReportList.add(inventoryReport);
				                		
			                		} catch (Exception ex) {
			                			//Error occurred in Selenium
			                			
			                			inventoryReport.setProductID(productID_Trim2_SKU);
			                			
				                        if(cellHS2 != null) {
				                        	inventoryReport.setASIN(cellHS2.getStringCellValue());
				                        }
				                        
			                			inventoryReport.setURL(driver.getCurrentUrl());
			                			
			                			inventoryReport.setReason("Selenium error occurred");
			                			inventoryReport.setLine(String.valueOf(j+1));
			                			
			                			inventoryReportFailList.add(inventoryReport);
			                			
			                			continue;
			                		}
			                		
		                		}
	                		}
	                	}
                	}
                	
                	
            	}
            	
        	}
        	
        	file.close();
            driver.close();
            driver.quit();
            
        } catch(Exception ioe) {
            ioe.printStackTrace();
        } finally {
            result.put("SUCCESS", inventoryReportList);
            result.put("FAIL", inventoryReportFailList);
            result.put("SHEET", new ArrayList<String>(Arrays.asList(sheetName)));
        }
        
        return result;
        
    }
}

