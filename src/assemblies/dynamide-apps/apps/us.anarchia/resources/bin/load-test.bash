for n in  `seq 1 10`   ; do
    curl -s -U laramie:cooper1631 http://localhost:18080/tagonomy\?tag=/science/genetics/HumanMigration$n
done

   ##curl http://localhost:18080/tagonomy?tag=/science/genetics/HumanMigration
