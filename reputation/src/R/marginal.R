

# Files for simple: coeffs_Binomial_RS_r_0.9.csv ...
#Files for hier: coeffs_Multinomial_RS_10_hier.csv

coeffs10 <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/coeffs_Kalman_RS_10_hier.csv",head=TRUE,sep=",")
coeffs20 <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/coeffs_Kalman_RS_20_hier.csv",head=TRUE,sep=",")
coeffs40 <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/coeffs_Kalman_RS_40_hier.csv",head=TRUE,sep=",")
coeffs50 <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/coeffs_Kalman_RS_50_hier.csv",head=TRUE,sep=",")
coeffs60 <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/coeffs_Kalman_RS_60_hier.csv",head=TRUE,sep=",")
coeffs80 <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/coeffs_Kalman_RS_80_hier.csv",head=TRUE,sep=",")

head(coeffs)
raw <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/raw/train.csv",head=TRUE,sep=",") #train_special.csv for hier, train.csv for simple.
head(raw)

l2cats<-read.table(file="/Users/mkokkodi/git/reputation_informs/data/raw/l2categories.csv",head=TRUE,sep=",")
head(l2cats)


cats<- c(10,20,40,50,60,80)

cats10<-c(13,16,8,9,11,17,15)
cats20<-c(24,18,25,23,21,27,22,20,28,26)
cats40<-c(37,38,35,58,62,39,34)
cats50<-c(42,41,63,46,43,40)
cats60<-c(61,51,56,53,50,49,54,55,57,47,60,48)
cats80<-c(88,74,72,76,75,70,68,71,73,89,69)



computeMarginalEffects(l2cats,cats10,raw,coeffs10)
computeMarginalEffects(l2cats,cats20,raw,coeffs20)
computeMarginalEffects(l2cats,cats40,raw,coeffs40)
computeMarginalEffects(l2cats,cats50,raw,coeffs50)
computeMarginalEffects(l2cats,cats60,raw,coeffs60)
computeMarginalEffects(l2cats,cats80,raw,coeffs80)


#### Routines!!!
computeMarginalEffects<-function(l2cats,cats,raw,coeffs){
  cat(sprintf("\\bf Category "))
  for (cat in cats){
    cat(sprintf(" & \\bf %s  ",l2cats[l2cats$id==cat,2]))
  }
  cat("\\\\ \n \\midrule ")
for (i in 1:nrow(coeffs))
{
  cat(sprintf(" %s ",l2cats[l2cats$id==cats[i],2]))
  #column
  j <-2
for (cat in cats){
 if (cat==0){
   q <- mean((raw$score/5))
 }
 else{
  q <- mean((raw[raw$category==cat,]$score/5))
}

  me <- coeffs[i,][j] /(q * (1-q))
 cat(sprintf("& %.3f ", me))

 
  j <- j+1
}
  
  cat("\\\\ \n ")
}
}

#############################
