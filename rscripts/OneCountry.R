#Use facetGridWithLines for sensitivity analysis

setwd("~/emlab-generation/rscripts")
source("rConfig.R")
source("batchRunAnalysis.R")
bigDF<-getDataFrameForModelRun("~/Desktop/emLabGen/output/","HPCNYCtest3", "OneCountryRuns")
setwd(resultFolder)

bigDFbc <- read.csv("baseRun.csv")
bigDFbc <- addSupplyRatios(bigDFbc)
bigDFbc <- addSumOfVariablesByPrefixToDF(bigDFbc, "ProducerCash")
bigDFcm <- read.csv("FipRun1.csv")
#bigDFcm <- subset(bigDFcm, grepl("OneCountryCM-6-", runId))
bigDFcm <- addSupplyRatios(bigDFcm)
bigDFcm <- addSumOfVariablesByPrefixToDF(bigDFcm, "ProducerCash")
bigDF <- rbind(bigDFbc, bigDFcm)
bigDFcm$modelRun <- "FeedInPremium"


bigDF$runId <- gsub("baseRun","BaseCase.",bigDF$runId)
bigDF$runId <- gsub("scenarioBiasFactorSensitivity","scenario.",bigDF$runId)
library(stringr)
bigDF$runId <- str_replace(bigDF$runId,"\\..*","")
bigDF$modelRun <- bigDF$runId

setwd("~/Desktop/emlabGen/analysis")

graphics.off()
library(gridExtra)
library(TeachingDemos)  

#Average Price
avgPricePlotinB_CI<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF, "Avg_El_PricesinEURpMWh_Country.A", "Avg. Electricity Price in Country B [EUR/MW]")
avgPricePlotinB_CI
ggsave(filename="AveragePricePlotinGermany.pdf",plot=avgPricePlotinB_CI, width=10, height=20, units="cm")

#SupplyRatio
supplyRatioB<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF, "SupplyRatio_Country.A", "Peak Capacity Supply Ratio B")
supplyRatioB
ggsave(filename="SupplyRatioB.pdf",plot=supplyRatioB, width=20, height=9, units="cm")

#ESM-Graph-Spaghetti
PriceSeg1PlotinBSpaghetti<-plotSpaghettiTimeSeries(bigDF, "PriceInEURperMWh_Segment.Country.A.1","Segment 1","Time [a]", NULL, 8)
PriceSeg1PlotinBSpaghetti

PriceSeg2PlotinBSpaghetti<-plotSpaghettiTimeSeries(bigDF, "PriceInEURperMWh_Segment.Country.A.2","Segment 2","Time [a]", NULL, 8)
PriceSeg2PlotinBSpaghetti

PriceSeg3PlotinBSpaghetti<-plotSpaghettiTimeSeries(bigDF, "PriceInEURperMWh_Segment.Country.A.3","Segment 3","Time [a]", NULL, 8)
PriceSeg3PlotinBSpaghetti

PriceSeg4PlotinBSpaghetti<-plotSpaghettiTimeSeries(bigDF, "PriceInEURperMWh_Segment.Country.A.4","Segment 4","Time [a]", NULL, 8)
PriceSeg4PlotinBSpaghetti

PricePlotB<- arrangeGrob(PriceSeg1PlotinBSpaghetti, PriceSeg2PlotinBSpaghetti, PriceSeg3PlotinBSpaghetti, PriceSeg4PlotinBSpaghetti, nrow = 4, ncol = 1)
ggsave(filename="PricePlotB.pdf",plot=PricePlotB, width=18, height=23, units="cm")

#ProducerCash
aggregateProducerCash<-plotTimeSeriesWithConfidenceIntervalGroupedInOnePlot(bigDF, "ProducerCashSum", "Aggregated Producer Cash")
aggregateProducerCash
ggsave(filename="ProducerCashSum.pdf",plot=aggregateProducerCash, width=20, height=9, units="cm")

aggregateProfit <- plotTimeSeriesWithConfidenceIntervalGroupedInOnePlot(bigDF, "AggregateFinances_Profit", "Aggregated Profit")
aggregateProfit
ggsave(filename="AggregateProfit.pdf",plot=aggregateProfit, width=20, height=9, units="cm")

#Consumer Expenditure
ConsumerExp<-plotTimeSeriesWithConfidenceIntervalByFacettedGroup(bigDF, "ConsumerExpenditure_Country.B.electricity.spot.market", "Consumer Expenditure")
ConsumerExp
ggsave(filename="ConsumerExp.pdf",plot=ConsumerExp, width=20, height=9, units="cm")

#Capacities
moltenCapacities<-meltTechnologyVariable(bigDF,"CapacityinMW_")
moltenCapacities$value<-moltenCapacities$value/1000
stackedCapacities<-plotStackedTechnologyDiagram(moltenVariable=moltenCapacities,ylabel="Capacity [GW]")
stackedCapacities
ggsave(filename="StackedCapacityDiagram.pdf",plot=stackedCapacities, width=20, height=15, units="cm")

#CapacityPrice
capacityPrice<-plotSpaghettiTimeSeries(bigDFcm, "CapacityClearingPointPriceinEur", "Capacity Clearing Price","Time [a]", NULL, 8)
capacityPrice
ggsave(filename="CapacityPrice.pdf",plot=capacityPrice, width=20, height=10, units="cm")

#CapacityVolume
capacityVolume<-plotSpaghettiTimeSeries(bigDFcm, "CapacityClearingPointVolumeinEur", "Capacity Clearing Volume","Time [a]", NULL, 8)
capacityVolume
ggsave(filename="CapacityVolume.pdf",plot=capacityVolume, width=20, height=10, units="cm")


AvgPriceMeanBC = 0
SupplyRatioMeanBC = 0
ConsumerExpMeanBC = 0
AggProducerProfitMeanBC = 0
ProducerCashSumBC = 0 

AvgPriceMeanCM = 0
SupplyRatioMeanCM = 0
ConsumerExpMeanCM = 0
AggProducerProfitMeanCM = 0
ProducerCashSumCM = 0 
CapacityPriceMean =0
CapacityVolumeMean = 0

shareOCGT_BC = 0
shareCCGT_BC = 0
shareOCGT_CM = 0
shareCCGT_CM = 0

#for(i in 1:120){
i=1
#bigDFbc$runId <- paste(bigDFbc$runId,"-",sep="")
#strTemp <- paste("CmPaper-1",i,"-",sep="")
#Temp <-  subset(bigDFbc, grepl(strTemp, runId))
Temp <- bigDFbc
AvgPriceMeanBC[i] <- mean(Temp$Avg_El_PricesinEURpMWh_Country.B)
SupplyRatioMeanBC[i]<- mean(Temp$SupplyRatio_Country.B)
ConsumerExpMeanBC[i]<-mean(Temp$ConsumerExpenditure_Country.B.electricity.spot.market)
AggProducerProfitMeanBC[i]<-mean(Temp$AggregateFinances_Profit)
ProducerCashSumBC[i]<-mean(Temp$ProducerCashSum)
shareOCGT_BC[i]<mean(Temp$CapacityinMW_OCGT/Temp$TotalOperationalCapacityPerZoneInMW_Country.B)
shareCCGT_BC[i]<mean(Temp$CapacityinMW_CCGT/Temp$TotalOperationalCapacityPerZoneInMW_Country.B)
#}

#for(i in 1:120){
#bigDFcm$runId <- paste(bigDFcm$runId,"-",sep="")
# strTemp <- paste("CmPaper-2",i,"-",sep="")
# Temp <-  subset(bigDFcm, grepl(strTemp, runId))
Temp <- bigDFcm
  AvgPriceMeanCM[i] <- mean(Temp$Avg_El_PricesinEURpMWh_Country.B)
  SupplyRatioMeanCM[i]<- mean(Temp$SupplyRatio_Country.B)
  ConsumerExpMeanCM[i]<-mean(Temp$ConsumerExpenditure_Country.B.electricity.spot.market)
  AggProducerProfitMeanCM[i]<-mean(Temp$AggregateFinances_Profit)
  ProducerCashSumCM[i]<-mean(Temp$ProducerCashSum)
  CapacityPriceMean[i]<-mean(Temp$CapacityClearingPointPriceinEur)
  CapacityVolumeMean[i]<-mean(Temp$CapacityClearingPointVolumeinEur)
  shareOCGT_CM[i]<mean(Temp$CapacityinMW_OCGT/Temp$TotalOperationalCapacityPerZoneInMW_Country.B)
  shareCCGT_CM[i]<mean(Temp$CapacityinMW_CCGT/Temp$TotalOperationalCapacityPerZoneInMW_Country.B)
#}


#OUTAGES
LOLE_CM = 0
LOLE_BC = 0

#Outage BaseCase
for(j in 1:50){
  Temp <- subset(bigDFbc, tick==j)
  VOLL1 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.1 == 2000)
  VOLL2 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.2 == 2000)
  VOLL3 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.3 == 2000)
  VOLL4 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.4 == 2000)
  VOLL5 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.5 == 2000)
  VOLL6 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.6 == 2000)
  LOLE_BC[j] = nrow(VOLL1)*10 + nrow(VOLL2)*50 + nrow(VOLL3)*576 +nrow(VOLL4)*576+ nrow(VOLL5)*576 + nrow(VOLL6)*576
}
LOLE_BC<-LOLE_BC/120
meanLOLE_BC <- mean(LOLE_BC)

#Outage CapacityMarket
for(j in 1:50){
  Temp <- subset(bigDFcm, tick==j)
  VOLL1 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.1 == 2000)
  VOLL2 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.2 == 2000)
  VOLL3 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.3 == 2000)
  VOLL4 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.4 == 2000)
  VOLL5 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.5 == 2000)
  VOLL6 <- subset(Temp, PriceInEURperMWh_Segment.Country.B.6 == 2000)
  LOLE_CM[j] = nrow(VOLL1)*10 + nrow(VOLL2)*50 + nrow(VOLL3)*576 +nrow(VOLL4)*576+ nrow(VOLL5)*576 + nrow(VOLL6)*576
}
LOLE_CM<-LOLE_CM/120
meanLOLE_CM <- mean(LOLE_CM)

pdf("LOLE.pdf",6,6)
par(mfrow=c(2,1))
plot(LOLE_BC, pch=20, main = "Base Case", xlab = "time(years)", ylab ="LOLE (hours)")
plot(LOLE_CM, pch=20, main = "Capacity Market", xlab = "time(years)", ylab ="LOLE (hours)")
dev.off()

#Shortage in the Capacity Market 
Shortage = matrix(data=NA,nrow=1,ncol=50)
meanShortage = 0

for(j in 1:50){
  Temp <- subset(bigDFcm, tick==j)
  Shortage[j] <- nrow(subset(Temp, CapacityClearingPointPriceinEur == 58940))
}
Shortage<-Shortage/120
meanShortage <- mean(Shortage)


#CHANGE IN PRODUCER CASH 
s1 <- subset(bigDFbc, tick==1)
s2 <- subset(bigDFbc, tick==49)
changeProducerCash <- (s2$ProducerCashSum - s1$ProducerCashSum)
pcChangeProducerCash <- changeProducerCash*100/s1$ProducerCashSum 
meanPercentageChangeProducerCashBC <- mean(pcChangeProducerCash)

s1 <- subset(bigDFcm, tick==1)
s2 <- subset(bigDFcm, tick==49)
changeProducerCash <- (s2$ProducerCashSum - s1$ProducerCashSum)
pcChangeProducerCash <- changeProducerCash*100/s1$ProducerCashSum 
meanPercentageChangeProducerCashCM <- mean(pcChangeProducerCash)


#Writing Results onto a Data Table

DataTable <- c(mean(AvgPriceMeanBC), sd(AvgPriceMeanBC), mean(AvgPriceMeanCM), sd(AvgPriceMeanCM))
DataTable <- rbind(DataTable, c(mean(SupplyRatioMeanBC), sd(SupplyRatioMeanBC), mean(SupplyRatioMeanCM), sd(SupplyRatioMeanCM)))
DataTable <- rbind(DataTable, c(mean(ConsumerExpMeanBC), sd(ConsumerExpMeanBC), mean(ConsumerExpMeanCM), sd(ConsumerExpMeanCM)))
DataTable <- rbind(DataTable, c(mean(ProducerCashSumBC), sd(ProducerCashSumBC), mean(ProducerCashSumCM), sd(ProducerCashSumCM)))
DataTable <- rbind(DataTable, c(mean(AggProducerProfitMeanBC), sd(AggProducerProfitMeanBC), mean(AggProducerProfitMeanCM), sd(AggProducerProfitMeanCM)))
DataTable <- rbind(DataTable, c(mean(shareOCGT_BC), sd(shareOCGT_BC), mean(shareOCGT_CM), sd(shareOCGT_CM)))
DataTable <- rbind(DataTable, c(mean(shareCCGT_BC), sd(shareCCGT_BC), mean(shareCCGT_CM), sd(shareCCGT_CM)))
DataTable <- rbind(DataTable, c(NA,NA,mean(CapacityPriceMean), sd(CapacityPriceMean)))
DataTable <- rbind(DataTable, c(NA,NA,mean(CapacityVolumeMean), sd(CapacityVolumeMean)))
DataTable <- rbind(DataTable, c(NA, meanLOLE_BC, NA, meanLOLE_CM))
DataTable <- rbind(DataTable, c(NA,NA,NA, meanShortage))
DataTable <- rbind(DataTable, c(NA,meanPercentageChangeProducerCashBC,NA, meanPercentageChangeProducerCashCM))

colnames(DataTable) <- c("BaseCase_Mean ", "BaseCase_SD", "CapacityMarket_Mean", "CapacityMarket_SD")
rownames(DataTable) <- c("AvgEletricityPrice", "SupplyRatio", "ConsumerExpenditure","ProducerCashSum", "AggregateProducerProfit","shareOCGT","shareCCGT", "CapacityPrice", "CapacityVolume","OutagesinHours","ShortageCM","PercentageChangeProducerCash")

write.csv(DataTable, "DataTable1.csv")
