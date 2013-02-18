coeffs <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/regression/coeffs_Kalman_PE_r__onTransitions.csv",head=TRUE,sep=",")
head(coeffs)
raw <- read.table(file="/Users/mkokkodi/git/reputation_informs/data/raw/train.csv",head=TRUE,sep=",")
head(raw)


cats<- c(0,10,20,40,50,60,80)
computeMarginalEffects(cats,raw)

computeMarginalEffects<-function(cats,raw){
  for (cat in cats){
    if(cat != 0)
    cat(sprintf("& %s  ",cat))
  }
  cat("\\\\ \n ")
  
i<-2
for (cat in cats){
 if (cat==0){
   q <- mean((raw$score/5))
 }
 else{
  q <- mean((raw[raw$category==cat,]$score/5))
}

  me <- coeffs[i] /(q * (1-q))
 cat(sprintf("%i ",cat))

 for(effect in me){
  cat(sprintf("& %f ", effect))
 }
 cat("\\\\ \n ")
  i <- 1+1
}
}

