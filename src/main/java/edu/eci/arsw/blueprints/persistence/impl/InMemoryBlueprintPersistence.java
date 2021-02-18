/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.blueprints.persistence.impl;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.persistence.BlueprintsPersistence;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/**
 *
 * @author hcadavid
 */
@Service("InMemoryBlueprintPersistence")
public class InMemoryBlueprintPersistence implements BlueprintsPersistence{
    private final ConcurrentHashMap<Tuple<String,String>,Blueprint> blueprints=new ConcurrentHashMap<>();
    public InMemoryBlueprintPersistence() {
        Point[] pts=new Point[]{new Point(140, 140),new Point(115, 115)};
        Point[] points= new Point[] {new Point(1,2),new Point(3,4),new Point(1,2)};
        Point[] pts2=new Point[]{new Point(14, 14),new Point(11, 15)};
        Blueprint bp=new Blueprint("Alejandro", "bp1",pts);
        Blueprint bp2=new Blueprint("David","bp2",points);
        Blueprint bp3=new Blueprint("David","bp3",pts2);
        blueprints.put(new Tuple<>(bp.getAuthor(),bp.getName()), bp);
        blueprints.put(new Tuple<>(bp2.getAuthor(),bp2.getName()), bp2);
        blueprints.put(new Tuple<>(bp3.getAuthor(),bp3.getName()), bp3);
    }     
    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        Blueprint blueprint= blueprints.putIfAbsent(new Tuple<>(bp.getAuthor(),bp.getName()), bp);
        if (blueprint!=null){
            throw new BlueprintPersistenceException("The given blueprint already exists: "+bp);
        }
    }
    @Override
    public  HashSet<Blueprint> getAllBlueprints(){
        return new HashSet<Blueprint>(blueprints.values());
    }
    @Override
    public void updateBlueprint(Blueprint bp,String author,String name) throws BlueprintNotFoundException {
        Blueprint oldbp=getBlueprint(author,name);
        oldbp.setPoints(bp.getPoints());
    }
    @Override
    public Blueprint getBlueprint(String author, String bprintname) throws BlueprintNotFoundException {
        Blueprint bp=blueprints.get(new Tuple<>(author, bprintname));
        if(bp==null)throw new BlueprintNotFoundException("El plano con estas caracteristicas no existe");
        return bp;
    }
    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException{
        Set<Blueprint> ans = new HashSet<>();
        for(Map.Entry<Tuple<String, String>, Blueprint> i :blueprints.entrySet()){
            if(i.getKey().o1.equals(author)){
                ans.add(i.getValue());
            }
        }
        if(ans.size()==0) throw new BlueprintNotFoundException("Este usuario no tiene planos");
        return ans;
    }
}