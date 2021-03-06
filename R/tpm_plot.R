#run in npTranscript results directory. Forms base for Shiny plot. 

library(reshape2)
library(ggplot2)
library(tidyr)
source('~/github/npTranscript/R/transcript_functions.R')


transcripts <- .readTranscripts('0.transcripts.txt.gz')
experiments <- attr(transcripts, 'info')
count_idx <- which(grepl(pattern = 'count[0-9]', x = colnames(transcripts)))
props <- prop.table(x = as.matrix(transcripts[,count_idx]), margin = 2)
colnames(props) <- experiments

#for use in 'split_by', if I can get it to work
time_vec <- c('2hpi','24hpi','48hpi')
exp_vec <- c('vero','calu','caco')

#calculate tpm
tpm <- props*1e-6
tpm <- cbind(ID=as.character(transcripts$ORFs),tpm)

#prep tpm_df
as.data.frame(tpm) %>% melt(id.vars='ID', measure.vars=experiments, value.name = 'TPM') %>%
	separate(variable, c('molecule_type', 'cell', 'time'), sep='_', remove = T) %>%
	transform( TPM = as.numeric(TPM), molecule_type = factor(molecule_type), cell = factor(cell), time = factor(time, levels = time_vec)) -> tpm_df

#input IDs in vector to plot
ggplot(subset(tpm_df, ID %in% c("leader_leader,N_end+", "leader_leader,ORF7a_end+")), aes(x=time, y=TPM, group=interaction(molecule_type, cell, ID), color = ID, linetype=molecule_type)) + geom_line() + scale_y_log10() + geom_point(inherit.aes=T,aes(shape = cell))

#split_by <- function(to_split, time_vec, exp_vec) {
#would need to loop over patterns rather than loop through rows }	
